package com.kgi.shredder.api.v1;

import com.kgi.shredder.api.v1.dto.DocumentFeedbackRequest;
import com.kgi.shredder.api.v1.dto.DocumentFeedbackResponse;
import com.kgi.shredder.config.SecurityContextUtil;
import com.kgi.shredder.domain.DocumentFeedback;
import com.kgi.shredder.service.collaboration.DocumentCollaborationService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/documents/{docId}/feedback")
public class CollaborationController {
    private final DocumentCollaborationService documentCollaborationService;

    public CollaborationController(DocumentCollaborationService documentCollaborationService) {
        this.documentCollaborationService = documentCollaborationService;
    }

    @GetMapping
    public List<DocumentFeedbackResponse> list(@PathVariable UUID docId) {
        return documentCollaborationService
                .listFeedback(SecurityContextUtil.currentTrainerId(), docId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @PostMapping
    public DocumentFeedbackResponse create(
            @PathVariable UUID docId,
            @Valid @RequestBody DocumentFeedbackRequest request
    ) {
        DocumentFeedback feedback = documentCollaborationService.addFeedback(
                SecurityContextUtil.currentTrainerId(),
                SecurityContextUtil.currentActorId(),
                docId,
                request.feedbackType(),
                request.comment()
        );
        return toResponse(feedback);
    }

    private DocumentFeedbackResponse toResponse(DocumentFeedback feedback) {
        return new DocumentFeedbackResponse(
                feedback.getFeedbackId(),
                feedback.getSourceDocument().getDocId(),
                feedback.getDocumentVersion() == null ? null : feedback.getDocumentVersion().getVersionId(),
                feedback.getActorId(),
                feedback.getFeedbackType().name(),
                feedback.getCommentText(),
                feedback.getStatus().name(),
                feedback.getCreatedAt()
        );
    }
}
