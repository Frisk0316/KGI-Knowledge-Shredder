package com.kgi.shredder.domain;

import com.kgi.shredder.domain.enums.ProcessingStatus;
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
@Table(name = "document_versions")
public class DocumentVersion {
    @Id
    @GeneratedValue
    @Column(name = "version_id")
    private UUID versionId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "doc_id", nullable = false)
    private SourceDocument sourceDocument;

    @Column(name = "version_number", nullable = false)
    private int versionNumber;

    @Column(name = "raw_text", nullable = false, columnDefinition = "TEXT")
    private String rawText;

    @Column(name = "redacted_text", columnDefinition = "TEXT")
    private String redactedText;

    @Column(name = "classification")
    private String classification;

    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status", nullable = false)
    private ProcessingStatus processingStatus = ProcessingStatus.PENDING;

    @Column(name = "parser_name", nullable = false)
    private String parserName;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    protected DocumentVersion() {
    }

    public DocumentVersion(SourceDocument sourceDocument, int versionNumber, String rawText, String parserName) {
        this.sourceDocument = sourceDocument;
        this.versionNumber = versionNumber;
        this.rawText = rawText;
        this.parserName = parserName;
    }

    public UUID getVersionId() {
        return versionId;
    }

    public SourceDocument getSourceDocument() {
        return sourceDocument;
    }

    public int getVersionNumber() {
        return versionNumber;
    }

    public String getRawText() {
        return rawText;
    }

    public String getRedactedText() {
        return redactedText;
    }

    public String getClassification() {
        return classification;
    }

    public ProcessingStatus getProcessingStatus() {
        return processingStatus;
    }

    public String getParserName() {
        return parserName;
    }

    public void markRedacting() {
        this.processingStatus = ProcessingStatus.REDACTING;
    }

    public void applyStage1(String redactedText, String classification) {
        this.redactedText = redactedText;
        this.classification = classification;
        this.processingStatus = ProcessingStatus.CHUNKING;
        this.failureReason = null;
    }

    public void markVectorizing() {
        this.processingStatus = ProcessingStatus.VECTORIZING;
    }

    public void markReady() {
        this.processingStatus = ProcessingStatus.READY;
        this.failureReason = null;
    }

    public void markFailed(String failureReason) {
        this.processingStatus = ProcessingStatus.FAILED;
        this.failureReason = failureReason;
    }
}
