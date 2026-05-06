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
@Table(name = "document_chunks")
public class DocumentChunk {
    @Id
    @GeneratedValue
    @Column(name = "chunk_id")
    private UUID chunkId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "version_id", nullable = false)
    private DocumentVersion documentVersion;

    @Column(name = "chunk_index", nullable = false)
    private int chunkIndex;

    @Column(name = "chunk_text", nullable = false, columnDefinition = "TEXT")
    private String chunkText;

    @Column(name = "chunk_strategy", nullable = false)
    private String chunkStrategy;

    @Column(name = "vector_store_id")
    private UUID vectorStoreId;

    protected DocumentChunk() {
    }

    public DocumentChunk(DocumentVersion documentVersion, int chunkIndex, String chunkText, String chunkStrategy) {
        this.documentVersion = documentVersion;
        this.chunkIndex = chunkIndex;
        this.chunkText = chunkText;
        this.chunkStrategy = chunkStrategy;
    }

    public UUID getChunkId() {
        return chunkId;
    }

    public DocumentVersion getDocumentVersion() {
        return documentVersion;
    }

    public int getChunkIndex() {
        return chunkIndex;
    }

    public String getChunkText() {
        return chunkText;
    }

    public String getChunkStrategy() {
        return chunkStrategy;
    }

    public UUID getVectorStoreId() {
        return vectorStoreId;
    }

    public void markVectorStored(UUID vectorStoreId) {
        this.vectorStoreId = vectorStoreId;
    }
}
