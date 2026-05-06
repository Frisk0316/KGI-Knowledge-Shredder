package com.kgi.shredder.service.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kgi.shredder.config.properties.KgiProperties;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class OpenAiCheckpointValidationServiceTest {
    @Test
    void deterministicCheckpointPassesValidModulesWithoutApiKey() {
        KgiProperties properties = new KgiProperties(
                new KgiProperties.Ai("text-embedding-3-small", 1536, 5),
                new KgiProperties.Checkpoint(true, 0.75, "openai", "openai-gpt"),
                new KgiProperties.Chunking(600),
                new KgiProperties.Async(1, 1),
                new KgiProperties.Security("trainer_001", "", "", "knowledge-shredder", "trainer_id"),
                new KgiProperties.Retention(365)
        );
        OpenAiCheckpointValidationService service = new OpenAiCheckpointValidationService(
                RestClient.builder(),
                new ObjectMapper(),
                properties,
                "",
                "gpt-4o"
        );
        MicroModuleSet moduleSet = new MicroModuleSet("summary", List.of(
                new MicroModuleSet.MicroModuleItem(1, "Title", "Content", "Takeaway", BigDecimal.valueOf(2), List.of())
        ));

        CheckpointValidationResult result = service.validate("Source text without email.", moduleSet);

        assertThat(result.passed()).isTrue();
        assertThat(result.overallScore()).isEqualByComparingTo(BigDecimal.ONE);
    }
}
