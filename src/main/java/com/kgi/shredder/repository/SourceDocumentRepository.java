package com.kgi.shredder.repository;

import com.kgi.shredder.domain.SourceDocument;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SourceDocumentRepository extends JpaRepository<SourceDocument, UUID> {
    Optional<SourceDocument> findByTrainerTrainerIdAndContentHashAndDeletedFalse(String trainerId, String contentHash);

    Page<SourceDocument> findByTrainerTrainerIdAndDeletedFalse(String trainerId, Pageable pageable);

    Optional<SourceDocument> findByDocIdAndTrainerTrainerIdAndDeletedFalse(UUID docId, String trainerId);

    Optional<SourceDocument> findByDocIdAndDeletedFalse(UUID docId);

    List<SourceDocument> findByDeletedTrueAndUpdatedAtBefore(OffsetDateTime cutoff);
}
