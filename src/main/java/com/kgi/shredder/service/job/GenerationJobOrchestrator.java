package com.kgi.shredder.service.job;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kgi.shredder.domain.DocumentDomainMap;
import com.kgi.shredder.domain.DocumentIncident;
import com.kgi.shredder.domain.DocumentVersion;
import com.kgi.shredder.domain.GenerationJob;
import com.kgi.shredder.domain.SourceDocument;
import com.kgi.shredder.domain.enums.EventType;
import com.kgi.shredder.domain.enums.GenerationJobStatus;
import com.kgi.shredder.exception.ResourceNotFoundException;
import com.kgi.shredder.repository.DocumentDomainMapRepository;
import com.kgi.shredder.repository.DocumentIncidentRepository;
import com.kgi.shredder.repository.DocumentVersionRepository;
import com.kgi.shredder.repository.GenerationJobRepository;
import com.kgi.shredder.service.ai.CheckpointValidationResult;
import com.kgi.shredder.service.ai.CheckpointValidationService;
import com.kgi.shredder.service.ai.DocumentVectorizationService;
import com.kgi.shredder.service.ai.MicroModuleBusinessValidator;
import com.kgi.shredder.service.ai.MicroModulePersistenceService;
import com.kgi.shredder.service.ai.MicroModuleSet;
import com.kgi.shredder.service.ai.Stage1RedactionService;
import com.kgi.shredder.service.ai.Stage1Result;
import com.kgi.shredder.service.ai.Stage2GenerationService;
import com.kgi.shredder.service.audit.AuditService;
import com.kgi.shredder.service.document.DocumentIngestionService;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class GenerationJobOrchestrator {
    private final GenerationJobRepository generationJobRepository;
    private final DocumentVersionRepository documentVersionRepository;
    private final DocumentDomainMapRepository documentDomainMapRepository;
    private final DocumentIncidentRepository documentIncidentRepository;
    private final DocumentIngestionService documentIngestionService;
    private final Stage1RedactionService stage1RedactionService;
    private final Stage2GenerationService stage2GenerationService;
    private final CheckpointValidationService checkpointValidationService;
    private final MicroModuleBusinessValidator microModuleBusinessValidator;
    private final MicroModulePersistenceService microModulePersistenceService;
    private final DocumentVectorizationService documentVectorizationService;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;
    private final Executor aiPipelineExecutor;

    public GenerationJobOrchestrator(
            GenerationJobRepository generationJobRepository,
            DocumentVersionRepository documentVersionRepository,
            DocumentDomainMapRepository documentDomainMapRepository,
            DocumentIncidentRepository documentIncidentRepository,
            DocumentIngestionService documentIngestionService,
            Stage1RedactionService stage1RedactionService,
            Stage2GenerationService stage2GenerationService,
            CheckpointValidationService checkpointValidationService,
            MicroModuleBusinessValidator microModuleBusinessValidator,
            MicroModulePersistenceService microModulePersistenceService,
            DocumentVectorizationService documentVectorizationService,
            AuditService auditService,
            ObjectMapper objectMapper,
            PlatformTransactionManager transactionManager,
            @Qualifier("aiPipelineExecutor") Executor aiPipelineExecutor
    ) {
        this.generationJobRepository = generationJobRepository;
        this.documentVersionRepository = documentVersionRepository;
        this.documentDomainMapRepository = documentDomainMapRepository;
        this.documentIncidentRepository = documentIncidentRepository;
        this.documentIngestionService = documentIngestionService;
        this.stage1RedactionService = stage1RedactionService;
        this.stage2GenerationService = stage2GenerationService;
        this.checkpointValidationService = checkpointValidationService;
        this.microModuleBusinessValidator = microModuleBusinessValidator;
        this.microModulePersistenceService = microModulePersistenceService;
        this.documentVectorizationService = documentVectorizationService;
        this.auditService = auditService;
        this.objectMapper = objectMapper;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.aiPipelineExecutor = aiPipelineExecutor;
    }

    public GenerationJob queueReprocess(String trainerId, UUID docId) {
        SourceDocument sourceDocument = documentIngestionService.getActiveDocument(trainerId, docId);
        GenerationJob job = generationJobRepository.save(
                new GenerationJob(sourceDocument, sourceDocument.getCurrentVersion(), trainerId)
        );
        auditService.log(EventType.JOB_QUEUED, job.getJobId().toString(), trainerId, Map.of("doc_id", docId));
        aiPipelineExecutor.execute(() -> runJob(job.getJobId()));
        return job;
    }

    public GenerationJob getJob(String trainerId, UUID jobId) {
        return generationJobRepository.findByJobIdAndTrainerId(jobId, trainerId)
                .orElseThrow(() -> new ResourceNotFoundException("Generation job not found."));
    }

    public void runJob(UUID jobId) {
        transactionTemplate.executeWithoutResult(status -> {
            GenerationJob job = generationJobRepository.findById(jobId)
                    .orElseThrow(() -> new ResourceNotFoundException("Generation job not found."));
            try {
                SourceDocument document = job.getSourceDocument();
                DocumentVersion version = job.getDocumentVersion();

                job.moveTo(GenerationJobStatus.STAGE1_REDACTING);
                version.markRedacting();
                Stage1Result stage1 = stage1RedactionService.redactAndClassify(version.getRawText());
                version.applyStage1(stage1.redactedText(), stage1.classification());
                job.recordStage1(writeJson(stage1));
                auditService.log(EventType.PII_REDACTED, version.getVersionId().toString(), job.getTrainerId(), Map.of(
                        "classification", stage1.classification(),
                        "pii_found", stage1.piiFound(),
                        "compression_ratio", stage1.compressionRatio()
                ));

                job.moveTo(GenerationJobStatus.STAGE2_GENERATING);
                MicroModuleSet moduleSet = stage2GenerationService.generateModules(
                        stage1.redactedText(),
                        documentDomainMapRepository.findBySourceDocumentDocId(document.getDocId()).stream()
                                .map(DocumentDomainMap::getKnowledgeDomain)
                                .map(domain -> domain.getDomainName())
                                .toList()
                );
                microModuleBusinessValidator.validate(moduleSet);
                job.recordStage2(writeJson(moduleSet));
                auditService.log(EventType.MODULES_GENERATED, job.getJobId().toString(), job.getTrainerId(), Map.of(
                        "module_count", moduleSet.modules().size()
                ));

                job.moveTo(GenerationJobStatus.VALIDATING);
                CheckpointValidationResult validation = checkpointValidationService.validate(stage1.redactedText(), moduleSet);
                job.recordValidation(writeJson(validation), validation.passed());
                auditService.log(EventType.CHECKPOINT_COMPLETED, job.getJobId().toString(), job.getTrainerId(), Map.of(
                        "overall_score", validation.overallScore(),
                        "passed", validation.passed()
                ));
                if (!validation.passed()) {
                    microModulePersistenceService.replaceModules(version, job, moduleSet, validation.overallScore(), false);
                    createIncident(document, version, job, "CHECKPOINT_FAILED", "HIGH", Map.of(
                            "overall_score", validation.overallScore(),
                            "rationale", validation.rationale()
                    ));
                    version.markFailed("Checkpoint validation failed.");
                    job.fail("Checkpoint validation failed.");
                    auditService.log(EventType.JOB_FAILED, job.getJobId().toString(), job.getTrainerId(), Map.of("reason", job.getErrorMessage()));
                    return;
                }

                microModulePersistenceService.replaceModules(version, job, moduleSet, validation.overallScore(), true);
                job.moveTo(GenerationJobStatus.VECTORIZING);
                version.markVectorizing();
                int chunkCount = documentVectorizationService.vectorize(document, version).size();
                version.markReady();
                job.complete();
                documentVersionRepository.save(version);
                generationJobRepository.save(job);
                auditService.log(EventType.DOCUMENT_VECTORIZED, version.getVersionId().toString(), job.getTrainerId(), Map.of("chunk_count", chunkCount));
                auditService.log(EventType.JOB_COMPLETED, job.getJobId().toString(), job.getTrainerId(), Map.of("doc_id", document.getDocId()));
            } catch (Exception ex) {
                String message = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
                job.fail(message);
                job.getDocumentVersion().markFailed(message);
                createIncident(job.getSourceDocument(), job.getDocumentVersion(), job, "PIPELINE_FAILED", "HIGH", Map.of("error", message));
                generationJobRepository.save(job);
                auditService.log(EventType.JOB_FAILED, job.getJobId().toString(), job.getTrainerId(), Map.of("reason", message));
            }
        });
    }

    private void createIncident(
            SourceDocument document,
            DocumentVersion version,
            GenerationJob job,
            String type,
            String severity,
            Map<String, ?> details
    ) {
        documentIncidentRepository.save(new DocumentIncident(document, version, job.getTrainerId(), type, severity, details));
        auditService.log(EventType.INCIDENT_CREATED, document.getDocId().toString(), job.getTrainerId(), Map.of(
                "incident_type", type,
                "severity", severity
        ));
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to serialize pipeline payload.", ex);
        }
    }
}
