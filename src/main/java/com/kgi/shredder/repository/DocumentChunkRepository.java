package com.kgi.shredder.repository;

import com.kgi.shredder.domain.DocumentChunk;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, UUID> {
    void deleteByDocumentVersionVersionId(UUID versionId);
}
