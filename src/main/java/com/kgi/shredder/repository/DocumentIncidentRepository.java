package com.kgi.shredder.repository;

import com.kgi.shredder.domain.DocumentIncident;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentIncidentRepository extends JpaRepository<DocumentIncident, UUID> {
    Page<DocumentIncident> findByResolved(boolean resolved, Pageable pageable);
}
