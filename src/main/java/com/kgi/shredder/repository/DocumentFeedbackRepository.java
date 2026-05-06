package com.kgi.shredder.repository;

import com.kgi.shredder.domain.DocumentFeedback;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentFeedbackRepository extends JpaRepository<DocumentFeedback, UUID> {
    List<DocumentFeedback> findBySourceDocumentDocIdOrderByCreatedAtDesc(UUID docId);
}
