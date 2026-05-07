package com.kgi.shredder.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "document_domain_map")
public class DocumentDomainMap {
    @Id
    @GeneratedValue
    @Column(name = "map_id")
    private UUID mapId;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "doc_id", nullable = false)
    private SourceDocument sourceDocument;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "domain_id", nullable = false)
    private KnowledgeDomain knowledgeDomain;

    protected DocumentDomainMap() {
    }

    public DocumentDomainMap(SourceDocument sourceDocument, KnowledgeDomain knowledgeDomain) {
        this.sourceDocument = sourceDocument;
        this.knowledgeDomain = knowledgeDomain;
    }

    public KnowledgeDomain getKnowledgeDomain() {
        return knowledgeDomain;
    }
}
