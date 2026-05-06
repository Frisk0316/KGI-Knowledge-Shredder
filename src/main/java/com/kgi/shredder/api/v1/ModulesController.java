package com.kgi.shredder.api.v1;

import com.kgi.shredder.api.v1.dto.ModuleAttemptRequest;
import com.kgi.shredder.api.v1.dto.ModuleAttemptResponse;
import com.kgi.shredder.config.SecurityContextUtil;
import com.kgi.shredder.service.learning.LearningFeedbackService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/modules")
public class ModulesController {
    private final LearningFeedbackService learningFeedbackService;

    public ModulesController(LearningFeedbackService learningFeedbackService) {
        this.learningFeedbackService = learningFeedbackService;
    }

    @PostMapping("/{moduleId}/attempts")
    public ModuleAttemptResponse recordAttempt(
            @PathVariable UUID moduleId,
            @Valid @RequestBody ModuleAttemptRequest request
    ) {
        var result = learningFeedbackService.recordAttempt(
                SecurityContextUtil.currentTrainerId(),
                moduleId,
                request.learnerId(),
                request.score(),
                request.interactionSeconds()
        );
        return new ModuleAttemptResponse(result.attemptId(), result.memoryStateId(), result.nextReviewAt());
    }
}
