package com.kgi.shredder.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "knowledge_domains")
public class KnowledgeDomain {
    @Id
    @Column(name = "domain_id")
    private Long domainId;

    @Column(name = "domain_name", nullable = false, unique = true)
    private String domainName;

    @Column(name = "description", nullable = false)
    private String description;

    protected KnowledgeDomain() {
    }

    public Long getDomainId() {
        return domainId;
    }

    public String getDomainName() {
        return domainName;
    }

    public String getDescription() {
        return description;
    }
}
