package com.kgi.shredder.service.learning;

import com.kgi.shredder.domain.LearnerMemoryState;
import com.kgi.shredder.domain.MicroModule;
import com.kgi.shredder.domain.ModuleAttempt;
import com.kgi.shredder.domain.enums.EventType;
import com.kgi.shredder.exception.ResourceNotFoundException;
import com.kgi.shredder.repository.LearnerMemoryStateRepository;
import com.kgi.shredder.repository.MicroModuleRepository;
import com.kgi.shredder.repository.ModuleAttemptRepository;
import com.kgi.shredder.service.audit.AuditService;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class LearningFeedbackService {
    private final MicroModuleRepository microModuleRepository;
    private final ModuleAttemptRepository moduleAttemptRepository;
    private final LearnerMemoryStateRepository learnerMemoryStateRepository;
    private final AuditService auditService;

    public LearningFeedbackService(
            MicroModuleRepository microModuleRepository,
            ModuleAttemptRepository moduleAttemptRepository,
            LearnerMemoryStateRepository learnerMemoryStateRepository,
            AuditService auditService
    ) {
        this.microModuleRepository = microModuleRepository;
        this.moduleAttemptRepository = moduleAttemptRepository;
        this.learnerMemoryStateRepository = learnerMemoryStateRepository;
        this.auditService = auditService;
    }

    @Transactional
    public AttemptResult recordAttempt(String trainerId, UUID moduleId, String learnerId, BigDecimal score, int interactionSeconds) {
        MicroModule module = microModuleRepository.findByModuleIdAndDocumentVersionSourceDocumentTrainerTrainerId(moduleId, trainerId)
                .orElseThrow(() -> new ResourceNotFoundException("Micro module not found."));
        ModuleAttempt attempt = moduleAttemptRepository.save(new ModuleAttempt(module, learnerId, trainerId, score, interactionSeconds));
        LearnerMemoryState state = learnerMemoryStateRepository.findByMicroModuleModuleIdAndLearnerId(moduleId, learnerId)
                .orElseGet(() -> new LearnerMemoryState(module, learnerId));
        state.applyAttempt(score);
        LearnerMemoryState savedState = learnerMemoryStateRepository.save(state);
        auditService.log(EventType.MODULE_ATTEMPT_RECORDED, moduleId.toString(), trainerId, Map.of(
                "learner_id", learnerId,
                "score", score,
                "interaction_seconds", interactionSeconds
        ));
        return new AttemptResult(attempt.getAttemptId(), savedState.getStateId(), savedState.getNextReviewAt());
    }

    public List<LearnerMemoryState> dueReviews(String trainerId, String learnerId) {
        return learnerMemoryStateRepository
                .findByLearnerIdAndMicroModuleDocumentVersionSourceDocumentTrainerTrainerIdAndNextReviewAtLessThanEqualOrderByNextReviewAt(
                        learnerId,
                        trainerId,
                        OffsetDateTime.now()
                );
    }

    public record AttemptResult(UUID attemptId, UUID memoryStateId, OffsetDateTime nextReviewAt) {
    }
}
