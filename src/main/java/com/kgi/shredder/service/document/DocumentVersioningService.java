package com.kgi.shredder.service.document;

import com.kgi.shredder.domain.DocumentVersion;
import com.kgi.shredder.domain.SourceDocument;
import com.kgi.shredder.domain.enums.EventType;
import com.kgi.shredder.repository.DocumentVersionRepository;
import com.kgi.shredder.service.audit.AuditService;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class DocumentVersioningService {
    private final DocumentVersionRepository documentVersionRepository;
    private final AuditService auditService;

    public DocumentVersioningService(DocumentVersionRepository documentVersionRepository, AuditService auditService) {
        this.documentVersionRepository = documentVersionRepository;
        this.auditService = auditService;
    }

    public DocumentVersion createInitialVersion(SourceDocument sourceDocument, String rawText, String parserName) {
        DocumentVersion version = new DocumentVersion(sourceDocument, 1, rawText, parserName);
        DocumentVersion saved = documentVersionRepository.save(version);
        auditService.log(
                EventType.VERSION_CREATED,
                saved.getVersionId().toString(),
                sourceDocument.getTrainer().getTrainerId(),
                Map.of("doc_id", sourceDocument.getDocId(), "version_number", 1)
        );
        return saved;
    }

    public DocumentVersion createReplacementVersion(SourceDocument sourceDocument, String rawText, String parserName) {
        int nextVersionNumber = documentVersionRepository
                .findBySourceDocumentDocIdOrderByVersionNumberDesc(sourceDocument.getDocId())
                .stream()
                .findFirst()
                .map(DocumentVersion::getVersionNumber)
                .orElse(0) + 1;
        DocumentVersion version = new DocumentVersion(sourceDocument, nextVersionNumber, rawText, parserName);
        DocumentVersion saved = documentVersionRepository.save(version);
        auditService.log(
                EventType.VERSION_CREATED,
                saved.getVersionId().toString(),
                sourceDocument.getTrainer().getTrainerId(),
                Map.of("doc_id", sourceDocument.getDocId(), "version_number", nextVersionNumber)
        );
        return saved;
    }
}
