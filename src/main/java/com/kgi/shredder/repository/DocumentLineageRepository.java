package com.kgi.shredder.repository;

import com.kgi.shredder.domain.DocumentLineage;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentLineageRepository extends JpaRepository<DocumentLineage, UUID> {
}
