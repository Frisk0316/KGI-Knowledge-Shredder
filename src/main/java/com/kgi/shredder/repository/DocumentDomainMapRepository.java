package com.kgi.shredder.repository;

import com.kgi.shredder.domain.DocumentDomainMap;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentDomainMapRepository extends JpaRepository<DocumentDomainMap, UUID> {
    List<DocumentDomainMap> findBySourceDocumentDocId(UUID docId);
}
