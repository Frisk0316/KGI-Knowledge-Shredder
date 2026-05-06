package com.kgi.shredder.api.v1;

import com.kgi.shredder.api.v1.dto.DueReviewResponse;
import com.kgi.shredder.config.SecurityContextUtil;
import com.kgi.shredder.service.learning.LearningFeedbackService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/learners")
public class LearnersController {
    private final LearningFeedbackService learningFeedbackService;

    public LearnersController(LearningFeedbackService learningFeedbackService) {
        this.learningFeedbackService = learningFeedbackService;
    }

    @GetMapping("/{learnerId}/reviews/due")
    public List<DueReviewResponse> dueReviews(@PathVariable String learnerId) {
        return learningFeedbackService.dueReviews(SecurityContextUtil.currentTrainerId(), learnerId).stream()
                .map(state -> new DueReviewResponse(
                        state.getStateId(),
                        state.getMicroModule().getModuleId(),
                        state.getMicroModule().getTitle(),
                        state.getNextReviewAt()
                ))
                .toList();
    }
}
