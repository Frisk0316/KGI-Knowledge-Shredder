package com.kgi.shredder.service.document;

import com.kgi.shredder.domain.DocumentDomainMap;
import com.kgi.shredder.domain.DocumentLineage;
import com.kgi.shredder.domain.DocumentVersion;
import com.kgi.shredder.domain.KnowledgeDomain;
import com.kgi.shredder.domain.SourceDocument;
import com.kgi.shredder.domain.Trainer;
import com.kgi.shredder.domain.enums.EventType;
import com.kgi.shredder.exception.BadRequestException;
import com.kgi.shredder.exception.DuplicateDocumentException;
import com.kgi.shredder.exception.ResourceNotFoundException;
import com.kgi.shredder.repository.DocumentDomainMapRepository;
import com.kgi.shredder.repository.DocumentLineageRepository;
import com.kgi.shredder.repository.KnowledgeDomainRepository;
import com.kgi.shredder.repository.SourceDocumentRepository;
import com.kgi.shredder.repository.TrainerRepository;
import com.kgi.shredder.service.audit.AuditService;
import jakarta.transaction.Transactional;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class DocumentIngestionService {
    private final TrainerRepository trainerRepository;
    private final KnowledgeDomainRepository knowledgeDomainRepository;
    private final SourceDocumentRepository sourceDocumentRepository;
    private final DocumentDomainMapRepository documentDomainMapRepository;
    private final DocumentLineageRepository documentLineageRepository;
    private final DocumentHashService documentHashService;
    private final TextExtractionService textExtractionService;
    private final DocumentVersioningService documentVersioningService;
    private final SafePreviewService safePreviewService;
    private final AuditService auditService;

    public DocumentIngestionService(
            TrainerRepository trainerRepository,
            KnowledgeDomainRepository knowledgeDomainRepository,
            SourceDocumentRepository sourceDocumentRepository,
            DocumentDomainMapRepository documentDomainMapRepository,
            DocumentLineageRepository documentLineageRepository,
            DocumentHashService documentHashService,
            TextExtractionService textExtractionService,
            DocumentVersioningService documentVersioningService,
            SafePreviewService safePreviewService,
            AuditService auditService
    ) {
        this.trainerRepository = trainerRepository;
        this.knowledgeDomainRepository = knowledgeDomainRepository;
        this.sourceDocumentRepository = sourceDocumentRepository;
        this.documentDomainMapRepository = documentDomainMapRepository;
        this.documentLineageRepository = documentLineageRepository;
        this.documentHashService = documentHashService;
        this.textExtractionService = textExtractionService;
        this.documentVersioningService = documentVersioningService;
        this.safePreviewService = safePreviewService;
        this.auditService = auditService;
    }

    @Transactional
    public UploadResult upload(String trainerId, MultipartFile file, List<Long> domainIds) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("file is required.");
        }
        Set<Long> normalizedDomainIds = new LinkedHashSet<>(domainIds == null ? List.of() : domainIds);
        if (normalizedDomainIds.isEmpty()) {
            throw new BadRequestException("At least one domain_id is required.");
        }

        byte[] fileBytes = read(file);
        String contentHash = documentHashService.sha256Hex(fileBytes);
        sourceDocumentRepository.findByTrainerTrainerIdAndContentHashAndDeletedFalse(trainerId, contentHash)
                .ifPresent(existing -> {
                    auditService.log(
                            EventType.DUPLICATE_DOCUMENT_REJECTED,
                            existing.getDocId().toString(),
                            trainerId,
                            Map.of("content_hash", contentHash)
                    );
                    throw new DuplicateDocumentException(existing.getDocId());
                });

        List<KnowledgeDomain> domains = knowledgeDomainRepository.findByDomainIdIn(normalizedDomainIds);
        if (domains.size() != normalizedDomainIds.size()) {
            throw new BadRequestException("One or more domain_ids do not exist.");
        }

        TextExtractionService.ParsedDocument parsed = textExtractionService.extract(fileBytes, file.getOriginalFilename());
        if (parsed.rawText().isBlank()) {
            throw new BadRequestException("The uploaded document does not contain extractable text.");
        }

        Trainer trainer = trainerRepository.findById(trainerId)
                .orElseGet(() -> trainerRepository.save(new Trainer(trainerId, trainerId)));
        SourceDocument sourceDocument = sourceDocumentRepository.save(
                new SourceDocument(trainer, safeFilename(file.getOriginalFilename()), contentHash)
        );
        DocumentVersion version = documentVersioningService.createInitialVersion(
                sourceDocument,
                parsed.rawText(),
                parsed.parserName()
        );
        sourceDocument.setCurrentVersion(version);
        sourceDocumentRepository.save(sourceDocument);
        domains.forEach(domain -> documentDomainMapRepository.save(new DocumentDomainMap(sourceDocument, domain)));

        auditService.log(
                EventType.DOCUMENT_UPLOADED,
                sourceDocument.getDocId().toString(),
                trainerId,
                Map.of(
                        "version_id", version.getVersionId(),
                        "domain_ids", normalizedDomainIds,
                        "content_hash", contentHash,
                        "filename", sourceDocument.getOriginalFilename()
                )
        );
        return new UploadResult(
                sourceDocument.getDocId(),
                version.getVersionId(),
                safePreviewService.preview(parsed.rawText())
        );
    }

    public Page<SourceDocument> list(String trainerId, Pageable pageable) {
        return sourceDocumentRepository.findByTrainerTrainerIdAndDeletedFalse(trainerId, pageable);
    }

    public SourceDocument getActiveDocument(String trainerId, UUID docId) {
        return sourceDocumentRepository.findByDocIdAndTrainerTrainerIdAndDeletedFalse(docId, trainerId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found."));
    }

    @Transactional
    public UploadResult replaceSource(String trainerId, UUID docId, MultipartFile file, List<Long> domainIds) {
        SourceDocument sourceDocument = getActiveDocument(trainerId, docId);
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("file is required.");
        }
        Set<Long> normalizedDomainIds = new LinkedHashSet<>(domainIds == null ? List.of() : domainIds);
        if (normalizedDomainIds.isEmpty()) {
            throw new BadRequestException("At least one domain_id is required.");
        }

        byte[] fileBytes = read(file);
        String contentHash = documentHashService.sha256Hex(fileBytes);
        sourceDocumentRepository.findByTrainerTrainerIdAndContentHashAndDeletedFalse(trainerId, contentHash)
                .filter(existing -> !existing.getDocId().equals(docId))
                .ifPresent(existing -> {
                    throw new DuplicateDocumentException(existing.getDocId());
                });

        List<KnowledgeDomain> domains = knowledgeDomainRepository.findByDomainIdIn(normalizedDomainIds);
        if (domains.size() != normalizedDomainIds.size()) {
            throw new BadRequestException("One or more domain_ids do not exist.");
        }

        TextExtractionService.ParsedDocument parsed = textExtractionService.extract(fileBytes, file.getOriginalFilename());
        if (parsed.rawText().isBlank()) {
            throw new BadRequestException("The uploaded document does not contain extractable text.");
        }

        DocumentVersion version = documentVersioningService.createReplacementVersion(
                sourceDocument,
                parsed.rawText(),
                parsed.parserName()
        );
        sourceDocument.replaceSource(safeFilename(file.getOriginalFilename()), contentHash, version);
        sourceDocumentRepository.save(sourceDocument);
        documentLineageRepository.save(new DocumentLineage(sourceDocument, sourceDocument, "SOURCE_REPLACED"));
        documentDomainMapRepository.deleteAll(documentDomainMapRepository.findBySourceDocumentDocId(docId));
        domains.forEach(domain -> documentDomainMapRepository.save(new DocumentDomainMap(sourceDocument, domain)));

        auditService.log(
                EventType.DOCUMENT_VERSION_REPLACED,
                sourceDocument.getDocId().toString(),
                trainerId,
                Map.of(
                        "version_id", version.getVersionId(),
                        "domain_ids", normalizedDomainIds,
                        "content_hash", contentHash,
                        "filename", sourceDocument.getOriginalFilename()
                )
        );
        return new UploadResult(
                sourceDocument.getDocId(),
                version.getVersionId(),
                safePreviewService.preview(parsed.rawText())
        );
    }

    @Transactional
    public void softDelete(String trainerId, UUID docId) {
        SourceDocument document = getActiveDocument(trainerId, docId);
        document.softDelete();
        sourceDocumentRepository.save(document);
        auditService.log(EventType.DOCUMENT_SOFT_DELETED, docId.toString(), trainerId, Map.of("doc_id", docId));
    }

    private byte[] read(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException ex) {
            throw new BadRequestException("Unable to read uploaded file.");
        }
    }

    private String safeFilename(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return "uploaded-document";
        }
        return originalFilename.replaceAll("[\\r\\n\\t]", "_");
    }

    public record UploadResult(UUID docId, UUID versionId, String previewText) {
    }
}
