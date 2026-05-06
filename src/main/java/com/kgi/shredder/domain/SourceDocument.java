package com.kgi.shredder.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "source_documents")
public class SourceDocument {
    @Id
    @GeneratedValue
    @Column(name = "doc_id")
    private UUID docId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trainer_id", nullable = false)
    private Trainer trainer;

    @Column(name = "original_filename", nullable = false)
    private String originalFilename;

    @Column(name = "content_hash", nullable = false, length = 64, columnDefinition = "CHAR(64)")
    @JdbcTypeCode(SqlTypes.CHAR)
    private String contentHash;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "current_version_id")
    private DocumentVersion currentVersion;

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    protected SourceDocument() {
    }

    public SourceDocument(Trainer trainer, String originalFilename, String contentHash) {
        this.trainer = trainer;
        this.originalFilename = originalFilename;
        this.contentHash = contentHash;
    }

    public UUID getDocId() {
        return docId;
    }

    public Trainer getTrainer() {
        return trainer;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public String getContentHash() {
        return contentHash;
    }

    public DocumentVersion getCurrentVersion() {
        return currentVersion;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setCurrentVersion(DocumentVersion currentVersion) {
        this.currentVersion = currentVersion;
        this.updatedAt = OffsetDateTime.now();
    }

    public void replaceSource(String originalFilename, String contentHash, DocumentVersion currentVersion) {
        this.originalFilename = originalFilename;
        this.contentHash = contentHash;
        this.currentVersion = currentVersion;
        this.updatedAt = OffsetDateTime.now();
    }

    public void softDelete() {
        this.deleted = true;
        this.updatedAt = OffsetDateTime.now();
    }
}
