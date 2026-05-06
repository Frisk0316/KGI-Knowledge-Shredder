package com.kgi.shredder.api.v1.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record DocumentFeedbackResponse(
        UUID feedbackId,
        UUID docId,
        UUID versionId,
        String actorId,
        String feedbackType,
        String comment,
        String status,
        OffsetDateTime createdAt
) {
}
