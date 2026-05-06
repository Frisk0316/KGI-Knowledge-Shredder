package com.kgi.shredder.service.ai;

import java.math.BigDecimal;
import java.util.Map;

public record CheckpointValidationResult(
        BigDecimal overallScore,
        boolean passed,
        Map<String, BigDecimal> dimensionScores,
        String rationale
) {
}
