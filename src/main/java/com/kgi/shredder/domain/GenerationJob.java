package com.kgi.shredder.domain;

import com.kgi.shredder.domain.enums.GenerationJobStatus;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "generation_jobs")
public class GenerationJob {
    @Id
    @GeneratedValue
    @Column(name = "job_id")
    private UUID jobId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "doc_id", nullable = false)
    private SourceDocument sourceDocument;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "version_id", nullable = false)
    private DocumentVersion documentVersion;

    @Column(name = "trainer_id", nullable = false)
    private String trainerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private GenerationJobStatus status = GenerationJobStatus.QUEUED;

    @Column(name = "stage1_output", columnDefinition = "TEXT")
    private String stage1Output;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "stage2_output", columnDefinition = "jsonb")
    private String stage2Output;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "validation_output", columnDefinition = "jsonb")
    private String validationOutput;

    @Column(name = "validation_passed")
    private Boolean validationPassed;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    protected GenerationJob() {
    }

    public GenerationJob(SourceDocument sourceDocument, DocumentVersion documentVersion, String trainerId) {
        this.sourceDocument = sourceDocument;
        this.documentVersion = documentVersion;
        this.trainerId = trainerId;
    }

    public UUID getJobId() {
        return jobId;
    }

    public GenerationJobStatus getStatus() {
        return status;
    }

    public SourceDocument getSourceDocument() {
        return sourceDocument;
    }

    public DocumentVersion getDocumentVersion() {
        return documentVersion;
    }

    public String getTrainerId() {
        return trainerId;
    }

    public String getStage1Output() {
        return stage1Output;
    }

    public String getStage2Output() {
        return stage2Output;
    }

    public String getValidationOutput() {
        return validationOutput;
    }

    public Boolean getValidationPassed() {
        return validationPassed;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void moveTo(GenerationJobStatus status) {
        this.status = status;
        this.updatedAt = OffsetDateTime.now();
    }

    public void recordStage1(String stage1Output) {
        this.stage1Output = stage1Output;
        this.updatedAt = OffsetDateTime.now();
    }

    public void recordStage2(String stage2Output) {
        this.stage2Output = stage2Output;
        this.updatedAt = OffsetDateTime.now();
    }

    public void recordValidation(String validationOutput, boolean validationPassed) {
        this.validationOutput = validationOutput;
        this.validationPassed = validationPassed;
        this.updatedAt = OffsetDateTime.now();
    }

    public void complete() {
        this.status = GenerationJobStatus.COMPLETED;
        this.errorMessage = null;
        this.updatedAt = OffsetDateTime.now();
    }

    public void fail(String errorMessage) {
        this.status = GenerationJobStatus.FAILED;
        this.errorMessage = errorMessage;
        this.updatedAt = OffsetDateTime.now();
    }
}
