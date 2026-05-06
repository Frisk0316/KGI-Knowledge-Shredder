package com.kgi.shredder.service.collaboration;

import com.kgi.shredder.domain.DocumentFeedback;
import com.kgi.shredder.domain.SourceDocument;
import com.kgi.shredder.domain.enums.DocumentFeedbackType;
import com.kgi.shredder.domain.enums.EventType;
import com.kgi.shredder.exception.BadRequestException;
import com.kgi.shredder.repository.DocumentFeedbackRepository;
import com.kgi.shredder.service.audit.AuditService;
import com.kgi.shredder.service.document.DocumentIngestionService;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class DocumentCollaborationService {
    private final DocumentIngestionService documentIngestionService;
    private final DocumentFeedbackRepository documentFeedbackRepository;
    private final AuditService auditService;

    public DocumentCollaborationService(
            DocumentIngestionService documentIngestionService,
            DocumentFeedbackRepository documentFeedbackRepository,
            AuditService auditService
    ) {
        this.documentIngestionService = documentIngestionService;
        this.documentFeedbackRepository = documentFeedbackRepository;
        this.auditService = auditService;
    }

    public List<DocumentFeedback> listFeedback(String workspaceTrainerId, UUID docId) {
        documentIngestionService.getActiveDocument(workspaceTrainerId, docId);
        return documentFeedbackRepository.findBySourceDocumentDocIdOrderByCreatedAtDesc(docId);
    }

    @Transactional
    public DocumentFeedback addFeedback(
            String workspaceTrainerId,
            String actorId,
            UUID docId,
            String rawType,
            String comment
    ) {
        SourceDocument document = documentIngestionService.getActiveDocument(workspaceTrainerId, docId);
        DocumentFeedbackType type = parseType(rawType);
        String normalizedComment = comment == null ? "" : comment.strip();
        if (type != DocumentFeedbackType.READ_MARK && normalizedComment.isBlank()) {
            throw new BadRequestException("comment is required for comments and change requests.");
        }
        DocumentFeedback saved = documentFeedbackRepository.save(new DocumentFeedback(
                document,
                document.getCurrentVersion(),
                actorId,
                type,
                normalizedComment.isBlank() ? null : normalizedComment
        ));
        auditService.log(eventType(type), docId.toString(), workspaceTrainerId, Map.of(
                "actor_id", actorId,
                "feedback_type", type.name(),
                "version_id", document.getCurrentVersion().getVersionId()
        ));
        return saved;
    }

    private DocumentFeedbackType parseType(String rawType) {
        try {
            return DocumentFeedbackType.valueOf(rawType.toUpperCase(Locale.ROOT));
        } catch (Exception ex) {
            throw new BadRequestException("feedback_type must be COMMENT, CHANGE_REQUEST, or READ_MARK.");
        }
    }

    private EventType eventType(DocumentFeedbackType type) {
        return switch (type) {
            case COMMENT -> EventType.DOCUMENT_FEEDBACK_CREATED;
            case CHANGE_REQUEST -> EventType.CHANGE_REQUEST_CREATED;
            case READ_MARK -> EventType.DOCUMENT_READ_MARKED;
        };
    }
}
