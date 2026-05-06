package com.kgi.shredder.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kgi.shredder.config.properties.KgiProperties;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class OpenAiCheckpointValidationService implements CheckpointValidationService {
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final KgiProperties properties;
    private final String apiKey;
    private final String model;

    public OpenAiCheckpointValidationService(
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper,
            KgiProperties properties,
            @Value("${spring.ai.openai.api-key:}") String apiKey,
            @Value("${spring.ai.openai.chat.options.model:gpt-4o}") String model
    ) {
        this.restClient = restClientBuilder.baseUrl("https://api.openai.com").build();
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.apiKey = apiKey;
        this.model = model;
    }

    @Override
    public CheckpointValidationResult validate(String redactedSource, MicroModuleSet moduleSet) {
        if (!properties.checkpoint().enabled()) {
            return passing("Checkpoint disabled by configuration.");
        }
        if (apiKey == null || apiKey.isBlank()) {
            return deterministic(redactedSource, moduleSet);
        }
        try {
            JsonNode response = restClient.post()
                    .uri("/v1/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .body(Map.of(
                            "model", model,
                            "response_format", Map.of("type", "json_object"),
                            "messages", List.of(
                                    Map.of("role", "system", "content", checkpointPrompt()),
                                    Map.of("role", "user", "content", objectMapper.writeValueAsString(Map.of(
                                            "source", redactedSource,
                                            "moduleSet", moduleSet
                                    )))
                            )
                    ))
                    .retrieve()
                    .body(JsonNode.class);
            String content = response.path("choices").path(0).path("message").path("content").asText();
            return objectMapper.readValue(content, CheckpointValidationResult.class);
        } catch (Exception ignored) {
            return deterministic(redactedSource, moduleSet);
        }
    }

    private String checkpointPrompt() {
        return """
                Return JSON only:
                {"overallScore":0.90,"passed":true,"dimensionScores":{"grounding":0.9,"tone":0.9,"pii":1.0,"sequence":1.0,"readingTime":1.0},"rationale":"string"}
                Validate factual grounding, tone/domain fit, PII leakage, unique sequence, and reading-time bounds.
                """;
    }

    private CheckpointValidationResult deterministic(String redactedSource, MicroModuleSet moduleSet) {
        boolean hasModules = moduleSet != null && moduleSet.modules() != null && !moduleSet.modules().isEmpty();
        boolean sequenceUnique = hasModules && moduleSet.modules().stream().map(MicroModuleSet.MicroModuleItem::sequenceOrder).distinct().count() == moduleSet.modules().size();
        boolean readingTimeOk = hasModules && moduleSet.modules().stream()
                .allMatch(module -> module.readingTimeMinutes().compareTo(BigDecimal.ONE) >= 0
                        && module.readingTimeMinutes().compareTo(BigDecimal.valueOf(7)) <= 0);
        boolean piiClean = redactedSource == null || !redactedSource.matches("(?s).*([A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}).*");
        BigDecimal score = BigDecimal.valueOf((hasModules ? 0.25 : 0.0)
                + (sequenceUnique ? 0.25 : 0.0)
                + (readingTimeOk ? 0.25 : 0.0)
                + (piiClean ? 0.25 : 0.0));
        boolean passed = score.doubleValue() >= properties.checkpoint().minScoreToPass();
        return new CheckpointValidationResult(
                score,
                passed,
                Map.of(
                        "grounding", hasModules ? BigDecimal.valueOf(0.80) : BigDecimal.ZERO,
                        "sequence", sequenceUnique ? BigDecimal.ONE : BigDecimal.ZERO,
                        "readingTime", readingTimeOk ? BigDecimal.ONE : BigDecimal.ZERO,
                        "pii", piiClean ? BigDecimal.ONE : BigDecimal.ZERO
                ),
                passed ? "Deterministic checkpoint passed." : "Deterministic checkpoint failed."
        );
    }

    private CheckpointValidationResult passing(String rationale) {
        return new CheckpointValidationResult(BigDecimal.ONE, true, Map.of("disabled", BigDecimal.ONE), rationale);
    }
}
