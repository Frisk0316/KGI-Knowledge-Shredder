package com.kgi.shredder.repository;

import com.kgi.shredder.domain.MicroModule;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MicroModuleRepository extends JpaRepository<MicroModule, UUID> {
    List<MicroModule> findByDocumentVersionSourceDocumentDocIdAndDocumentVersionSourceDocumentTrainerTrainerIdOrderBySequenceOrder(UUID docId, String trainerId);

    Optional<MicroModule> findByModuleIdAndDocumentVersionSourceDocumentTrainerTrainerId(UUID moduleId, String trainerId);

    void deleteByDocumentVersionVersionId(UUID versionId);
}
