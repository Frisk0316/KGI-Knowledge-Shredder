package com.kgi.shredder.api.v1.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record DocumentFeedbackRequest(
        @JsonProperty("feedback_type") @NotBlank String feedbackType,
        String comment
) {
}
