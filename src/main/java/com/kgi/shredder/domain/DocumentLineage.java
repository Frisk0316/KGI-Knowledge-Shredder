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
import java.util.UUID;

@Entity
@Table(name = "document_lineage")
public class DocumentLineage {
    @Id
    @GeneratedValue
    @Column(name = "lineage_id")
    private UUID lineageId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_doc_id", nullable = false)
    private SourceDocument sourceDoc;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "derived_doc_id", nullable = false)
    private SourceDocument derivedDoc;

    @Column(name = "relationship_type", nullable = false, length = 80)
    private String relationshipType;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    protected DocumentLineage() {
    }

    public DocumentLineage(SourceDocument sourceDoc, SourceDocument derivedDoc, String relationshipType) {
        this.sourceDoc = sourceDoc;
        this.derivedDoc = derivedDoc;
        this.relationshipType = relationshipType;
    }

    public UUID getLineageId() {
        return lineageId;
    }

    public SourceDocument getSourceDoc() {
        return sourceDoc;
    }

    public SourceDocument getDerivedDoc() {
        return derivedDoc;
    }

    public String getRelationshipType() {
        return relationshipType;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
