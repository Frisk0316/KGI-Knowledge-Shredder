package com.kgi.shredder.service.ai;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.kgi.shredder.exception.BadRequestException;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class MicroModuleBusinessValidatorTest {
    private final MicroModuleBusinessValidator validator = new MicroModuleBusinessValidator();

    @Test
    void acceptsValidModules() {
        MicroModuleSet moduleSet = new MicroModuleSet("summary", List.of(
                new MicroModuleSet.MicroModuleItem(1, "Title", "Content", "Takeaway", BigDecimal.valueOf(2), List.of(1L))
        ));

        assertDoesNotThrow(() -> validator.validate(moduleSet));
    }

    @Test
    void rejectsDuplicateSequence() {
        MicroModuleSet moduleSet = new MicroModuleSet("summary", List.of(
                new MicroModuleSet.MicroModuleItem(1, "Title", "Content", "Takeaway", BigDecimal.valueOf(2), List.of()),
                new MicroModuleSet.MicroModuleItem(1, "Other", "Content", "Takeaway", BigDecimal.valueOf(3), List.of())
        ));

        assertThrows(BadRequestException.class, () -> validator.validate(moduleSet));
    }

    @Test
    void rejectsReadingTimeOutsideBounds() {
        MicroModuleSet moduleSet = new MicroModuleSet("summary", List.of(
                new MicroModuleSet.MicroModuleItem(1, "Title", "Content", "Takeaway", BigDecimal.valueOf(8), List.of())
        ));

        assertThrows(BadRequestException.class, () -> validator.validate(moduleSet));
    }
}
