package com.kgi.shredder.service.ai;

import com.kgi.shredder.exception.BadRequestException;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class MicroModuleBusinessValidator {
    public void validate(MicroModuleSet moduleSet) {
        if (moduleSet == null || moduleSet.modules() == null || moduleSet.modules().isEmpty()) {
            throw new BadRequestException("At least one micro module is required.");
        }
        Set<Integer> sequences = new HashSet<>();
        for (MicroModuleSet.MicroModuleItem module : moduleSet.modules()) {
            if (!sequences.add(module.sequenceOrder())) {
                throw new BadRequestException("Micro module sequence_order must be unique.");
            }
            if (module.title() == null || module.title().isBlank()) {
                throw new BadRequestException("Micro module title is required.");
            }
            if (module.content() == null || module.content().isBlank()) {
                throw new BadRequestException("Micro module content is required.");
            }
            if (module.readingTimeMinutes() == null
                    || module.readingTimeMinutes().compareTo(BigDecimal.ONE) < 0
                    || module.readingTimeMinutes().compareTo(BigDecimal.valueOf(7)) > 0) {
                throw new BadRequestException("Micro module reading time must be between 1 and 7 minutes.");
            }
        }
    }
}
