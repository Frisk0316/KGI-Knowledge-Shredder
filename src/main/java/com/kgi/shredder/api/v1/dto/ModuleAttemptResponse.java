package com.kgi.shredder.api.v1.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ModuleAttemptResponse(
        UUID attemptId,
        UUID memoryStateId,
        OffsetDateTime nextReviewAt
) {
}
