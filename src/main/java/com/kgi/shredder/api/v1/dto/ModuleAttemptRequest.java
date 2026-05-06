package com.kgi.shredder.api.v1.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record ModuleAttemptRequest(
        @JsonProperty("learner_id") @NotBlank String learnerId,
        @NotNull @DecimalMin("0.0") @DecimalMax("1.0") BigDecimal score,
        @JsonProperty("interaction_seconds") @Min(1) int interactionSeconds
) {
}
