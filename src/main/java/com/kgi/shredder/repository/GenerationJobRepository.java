package com.kgi.shredder.repository;

import com.kgi.shredder.domain.GenerationJob;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GenerationJobRepository extends JpaRepository<GenerationJob, UUID> {
    Optional<GenerationJob> findByJobIdAndTrainerId(UUID jobId, String trainerId);
}
