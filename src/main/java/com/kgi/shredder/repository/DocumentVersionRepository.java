package com.kgi.shredder.repository;

import com.kgi.shredder.domain.DocumentVersion;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentVersionRepository extends JpaRepository<DocumentVersion, UUID> {
    int countBySourceDocumentDocId(UUID docId);

    List<DocumentVersion> findBySourceDocumentDocIdOrderByVersionNumberDesc(UUID docId);
}
