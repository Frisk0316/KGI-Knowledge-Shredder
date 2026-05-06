package com.kgi.shredder.api.v1.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record MicroModuleResponse(
        UUID moduleId,
        int sequenceOrder,
        String title,
        String content,
        String keyTakeaway,
        BigDecimal readingTimeMinutes,
        BigDecimal validationScore,
        boolean validated,
        List<Long> domainIds,
        OffsetDateTime createdAt
) {
}
