package com.kgi.shredder.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "document_incidents")
public class DocumentIncident {
    @Id
    @GeneratedValue
    @Column(name = "incident_id")
    private UUID incidentId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "doc_id", nullable = false)
    private SourceDocument sourceDocument;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "version_id")
    private DocumentVersion documentVersion;

    @Column(name = "trainer_id", nullable = false)
    private String trainerId;

    @Column(name = "incident_type", nullable = false)
    private String incidentType;

    @Column(name = "severity", nullable = false)
    private String severity;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "details", nullable = false, columnDefinition = "jsonb")
    private Map<String, ?> details;

    @Column(name = "resolved", nullable = false)
    private boolean resolved;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "resolved_at")
    private OffsetDateTime resolvedAt;

    protected DocumentIncident() {
    }

    public DocumentIncident(
            SourceDocument sourceDocument,
            DocumentVersion documentVersion,
            String trainerId,
            String incidentType,
            String severity,
            Map<String, ?> details
    ) {
        this.sourceDocument = sourceDocument;
        this.documentVersion = documentVersion;
        this.trainerId = trainerId;
        this.incidentType = incidentType;
        this.severity = severity;
        this.details = details;
    }

    public UUID getIncidentId() {
        return incidentId;
    }

    public String getIncidentType() {
        return incidentType;
    }

    public String getSeverity() {
        return severity;
    }

    public Map<String, ?> getDetails() {
        return details;
    }

    public boolean isResolved() {
        return resolved;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setResolved(boolean resolved) {
        this.resolved = resolved;
        this.resolvedAt = resolved ? OffsetDateTime.now() : null;
    }
}
