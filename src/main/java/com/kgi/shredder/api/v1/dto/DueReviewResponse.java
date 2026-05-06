package com.kgi.shredder.api.v1.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record DueReviewResponse(
        UUID stateId,
        UUID moduleId,
        String title,
        OffsetDateTime nextReviewAt
) {
}
