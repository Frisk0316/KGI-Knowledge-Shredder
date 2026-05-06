package com.kgi.shredder.service.ai;

import java.math.BigDecimal;
import java.util.List;

public record MicroModuleSet(String documentSummary, List<MicroModuleItem> modules) {
    public record MicroModuleItem(
            int sequenceOrder,
            String title,
            String content,
            String keyTakeaway,
            BigDecimal readingTimeMinutes,
            List<Long> domainIds
    ) {
    }
}
