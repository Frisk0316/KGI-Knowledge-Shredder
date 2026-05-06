package com.kgi.shredder.api.v1;

import com.kgi.shredder.api.v1.dto.DocumentDetailResponse;
import com.kgi.shredder.api.v1.dto.DocumentListResponse;
import com.kgi.shredder.api.v1.dto.DomainResponse;
import com.kgi.shredder.api.v1.dto.JobResponse;
import com.kgi.shredder.api.v1.dto.MicroModuleResponse;
import com.kgi.shredder.api.v1.dto.UploadDocumentResponse;
import com.kgi.shredder.config.SecurityContextUtil;
import com.kgi.shredder.domain.DocumentDomainMap;
import com.kgi.shredder.domain.DocumentVersion;
import com.kgi.shredder.domain.GenerationJob;
import com.kgi.shredder.domain.MicroModule;
import com.kgi.shredder.domain.SourceDocument;
import com.kgi.shredder.repository.DocumentDomainMapRepository;
import com.kgi.shredder.repository.DocumentVersionRepository;
import com.kgi.shredder.repository.MicroModuleRepository;
import com.kgi.shredder.service.document.DocumentIngestionService;
import com.kgi.shredder.service.job.GenerationJobOrchestrator;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/documents")
public class DocumentsController {
    private final DocumentIngestionService documentIngestionService;
    private final DocumentDomainMapRepository documentDomainMapRepository;
    private final DocumentVersionRepository documentVersionRepository;
    private final MicroModuleRepository microModuleRepository;
    private final GenerationJobOrchestrator generationJobOrchestrator;

    public DocumentsController(
            DocumentIngestionService documentIngestionService,
            DocumentDomainMapRepository documentDomainMapRepository,
            DocumentVersionRepository documentVersionRepository,
            MicroModuleRepository microModuleRepository,
            GenerationJobOrchestrator generationJobOrchestrator
    ) {
        this.documentIngestionService = documentIngestionService;
        this.documentDomainMapRepository = documentDomainMapRepository;
        this.documentVersionRepository = documentVersionRepository;
        this.microModuleRepository = microModuleRepository;
        this.generationJobOrchestrator = generationJobOrchestrator;
    }

    @PostMapping("/upload")
    public ResponseEntity<UploadDocumentResponse> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("domain_ids") List<Long> domainIds
    ) {
        var result = documentIngestionService.upload(SecurityContextUtil.currentTrainerId(), file, domainIds);
        return ResponseEntity.created(URI.create("/api/v1/documents/" + result.docId()))
                .body(new UploadDocumentResponse(result.docId(), result.versionId(), result.previewText()));
    }

    @GetMapping
    public Page<DocumentListResponse> list(Pageable pageable) {
        return documentIngestionService.list(SecurityContextUtil.currentTrainerId(), pageable)
                .map(document -> new DocumentListResponse(
                        document.getDocId(),
                        document.getOriginalFilename(),
                        document.getCurrentVersion().getVersionId()
                ));
    }

    @GetMapping("/{docId}")
    public DocumentDetailResponse get(@PathVariable UUID docId) {
        SourceDocument document = documentIngestionService.getActiveDocument(SecurityContextUtil.currentTrainerId(), docId);
        List<DomainResponse> domains = documentDomainMapRepository.findBySourceDocumentDocId(docId).stream()
                .map(DocumentDomainMap::getKnowledgeDomain)
                .map(domain -> new DomainResponse(domain.getDomainId(), domain.getDomainName(), domain.getDescription()))
                .toList();
        List<DocumentDetailResponse.VersionSummary> versions = documentVersionRepository
                .findBySourceDocumentDocIdOrderByVersionNumberDesc(docId)
                .stream()
                .map(this::toVersionSummary)
                .toList();
        return new DocumentDetailResponse(
                document.getDocId(),
                document.getOriginalFilename(),
                document.getContentHash(),
                document.getCurrentVersion().getVersionId(),
                document.getCurrentVersion().getProcessingStatus().name(),
                domains,
                versions
        );
    }

    @DeleteMapping("/{docId}")
    public ResponseEntity<Void> delete(@PathVariable UUID docId) {
        documentIngestionService.softDelete(SecurityContextUtil.currentTrainerId(), docId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{docId}/reprocess")
    public JobResponse reprocess(@PathVariable UUID docId) {
        GenerationJob job = generationJobOrchestrator.queueReprocess(SecurityContextUtil.currentTrainerId(), docId);
        return new JobResponse(job.getJobId(), job.getStatus().name(), job.getErrorMessage());
    }

    @PutMapping("/{docId}/source")
    public UploadDocumentResponse replaceSource(
            @PathVariable UUID docId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("domain_ids") List<Long> domainIds
    ) {
        var result = documentIngestionService.replaceSource(
                SecurityContextUtil.currentTrainerId(),
                docId,
                file,
                domainIds
        );
        return new UploadDocumentResponse(result.docId(), result.versionId(), result.previewText());
    }

    @GetMapping("/{docId}/modules")
    public List<MicroModuleResponse> modules(@PathVariable UUID docId) {
        documentIngestionService.getActiveDocument(SecurityContextUtil.currentTrainerId(), docId);
        return microModuleRepository
                .findByDocumentVersionSourceDocumentDocIdAndDocumentVersionSourceDocumentTrainerTrainerIdOrderBySequenceOrder(
                        docId,
                        SecurityContextUtil.currentTrainerId()
                )
                .stream()
                .map(this::toModuleResponse)
                .toList();
    }

    private DocumentDetailResponse.VersionSummary toVersionSummary(DocumentVersion version) {
        return new DocumentDetailResponse.VersionSummary(
                version.getVersionId(),
                version.getVersionNumber(),
                version.getProcessingStatus().name(),
                version.getParserName()
        );
    }

    private MicroModuleResponse toModuleResponse(MicroModule module) {
        return new MicroModuleResponse(
                module.getModuleId(),
                module.getSequenceOrder(),
                module.getTitle(),
                module.getModuleContent(),
                module.getKeyTakeaway(),
                module.getReadingTimeMinutes(),
                module.getValidationScore(),
                module.isValidated(),
                module.getDomainIds(),
                module.getCreatedAt()
        );
    }
}
