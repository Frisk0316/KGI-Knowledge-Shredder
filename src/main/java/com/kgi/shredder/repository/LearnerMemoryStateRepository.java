package com.kgi.shredder.repository;

import com.kgi.shredder.domain.LearnerMemoryState;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LearnerMemoryStateRepository extends JpaRepository<LearnerMemoryState, UUID> {
    Optional<LearnerMemoryState> findByMicroModuleModuleIdAndLearnerId(UUID moduleId, String learnerId);

    List<LearnerMemoryState> findByLearnerIdAndMicroModuleDocumentVersionSourceDocumentTrainerTrainerIdAndNextReviewAtLessThanEqualOrderByNextReviewAt(
            String learnerId,
            String trainerId,
            OffsetDateTime now
    );
}
