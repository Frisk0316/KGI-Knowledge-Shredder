package com.kgi.shredder.domain;

import com.kgi.shredder.domain.enums.DocumentFeedbackStatus;
import com.kgi.shredder.domain.enums.DocumentFeedbackType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "document_feedback")
public class DocumentFeedback {
    @Id
    @GeneratedValue
    @Column(name = "feedback_id")
    private UUID feedbackId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "doc_id", nullable = false)
    private SourceDocument sourceDocument;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "version_id")
    private DocumentVersion documentVersion;

    @Column(name = "actor_id", nullable = false)
    private String actorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "feedback_type", nullable = false)
    private DocumentFeedbackType feedbackType;

    @Column(name = "comment_text", columnDefinition = "TEXT")
    private String commentText;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private DocumentFeedbackStatus status = DocumentFeedbackStatus.OPEN;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "resolved_at")
    private OffsetDateTime resolvedAt;

    protected DocumentFeedback() {
    }

    public DocumentFeedback(
            SourceDocument sourceDocument,
            DocumentVersion documentVersion,
            String actorId,
            DocumentFeedbackType feedbackType,
            String commentText
    ) {
        this.sourceDocument = sourceDocument;
        this.documentVersion = documentVersion;
        this.actorId = actorId;
        this.feedbackType = feedbackType;
        this.commentText = commentText;
        if (feedbackType == DocumentFeedbackType.READ_MARK) {
            this.status = DocumentFeedbackStatus.READ;
        }
    }

    public UUID getFeedbackId() {
        return feedbackId;
    }

    public SourceDocument getSourceDocument() {
        return sourceDocument;
    }

    public DocumentVersion getDocumentVersion() {
        return documentVersion;
    }

    public String getActorId() {
        return actorId;
    }

    public DocumentFeedbackType getFeedbackType() {
        return feedbackType;
    }

    public String getCommentText() {
        return commentText;
    }

    public DocumentFeedbackStatus getStatus() {
        return status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getResolvedAt() {
        return resolvedAt;
    }
}
