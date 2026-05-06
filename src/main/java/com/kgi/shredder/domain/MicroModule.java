package com.kgi.shredder.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "micro_modules")
public class MicroModule {
    @Id
    @GeneratedValue
    @Column(name = "module_id")
    private UUID moduleId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "version_id", nullable = false)
    private DocumentVersion documentVersion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id")
    private GenerationJob generationJob;

    @Column(name = "sequence_order", nullable = false)
    private int sequenceOrder;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "module_content", nullable = false, columnDefinition = "TEXT")
    private String moduleContent;

    @Column(name = "key_takeaway", columnDefinition = "TEXT")
    private String keyTakeaway;

    @Column(name = "reading_time_minutes", nullable = false)
    private BigDecimal readingTimeMinutes;

    @Column(name = "validation_score", precision = 3, scale = 2)
    private BigDecimal validationScore;

    @Column(name = "validated", nullable = false)
    private boolean validated;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "domain_ids", nullable = false, columnDefinition = "jsonb")
    private List<Long> domainIds = List.of();

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    protected MicroModule() {
    }

    public MicroModule(
            DocumentVersion documentVersion,
            GenerationJob generationJob,
            int sequenceOrder,
            String title,
            String moduleContent,
            String keyTakeaway,
            BigDecimal readingTimeMinutes,
            BigDecimal validationScore,
            boolean validated,
            List<Long> domainIds
    ) {
        this.documentVersion = documentVersion;
        this.generationJob = generationJob;
        this.sequenceOrder = sequenceOrder;
        this.title = title;
        this.moduleContent = moduleContent;
        this.keyTakeaway = keyTakeaway;
        this.readingTimeMinutes = readingTimeMinutes;
        this.validationScore = validationScore;
        this.validated = validated;
        this.domainIds = domainIds == null ? List.of() : domainIds;
    }

    public UUID getModuleId() {
        return moduleId;
    }

    public DocumentVersion getDocumentVersion() {
        return documentVersion;
    }

    public GenerationJob getGenerationJob() {
        return generationJob;
    }

    public int getSequenceOrder() {
        return sequenceOrder;
    }

    public String getTitle() {
        return title;
    }

    public String getModuleContent() {
        return moduleContent;
    }

    public String getKeyTakeaway() {
        return keyTakeaway;
    }

    public BigDecimal getReadingTimeMinutes() {
        return readingTimeMinutes;
    }

    public BigDecimal getValidationScore() {
        return validationScore;
    }

    public boolean isValidated() {
        return validated;
    }

    public List<Long> getDomainIds() {
        return domainIds;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
