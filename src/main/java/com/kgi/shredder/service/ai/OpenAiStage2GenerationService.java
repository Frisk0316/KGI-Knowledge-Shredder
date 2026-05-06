package com.kgi.shredder.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class OpenAiStage2GenerationService implements Stage2GenerationService {
    private final RestClient restClient;
    private final BeanOutputConverter<MicroModuleSet> outputConverter;
    private final String apiKey;
    private final String model;

    public OpenAiStage2GenerationService(
            RestClient.Builder restClientBuilder,
            @Value("${spring.ai.openai.api-key:}") String apiKey,
            @Value("${spring.ai.openai.chat.options.model:gpt-4o}") String model
    ) {
        this.restClient = restClientBuilder.baseUrl("https://api.openai.com").build();
        this.outputConverter = new BeanOutputConverter<>(MicroModuleSet.class);
        this.apiKey = apiKey;
        this.model = model;
    }

    @Override
    public MicroModuleSet generateModules(String redactedText, List<String> domainNames) {
        if (apiKey == null || apiKey.isBlank()) {
            return fallback(redactedText, domainNames);
        }
        try {
            JsonNode response = restClient.post()
                    .uri("/v1/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .body(Map.of(
                            "model", model,
                            "response_format", Map.of("type", "json_object"),
                            "messages", List.of(
                                    Map.of("role", "system", "content", systemPrompt(domainNames)),
                                    Map.of("role", "user", "content", userPrompt(redactedText, domainNames))
                            )
                    ))
                    .retrieve()
                    .body(JsonNode.class);
            String content = response.path("choices").path(0).path("message").path("content").asText();
            return outputConverter.convert(content);
        } catch (Exception ignored) {
            return fallback(redactedText, domainNames);
        }
    }

    private String systemPrompt(List<String> domainNames) {
        String domains = domainNames == null || domainNames.isEmpty()
                ? "Financial Knowledge"
                : String.join(", ", domainNames);
        return """
                You are creating training content for the domains of [%s].
                Chunk the provided text into micro-learning modules (2-7 minute segments).
                Ensure the tone and emphasis match the target domains:
                - For CRM or client-facing domains, emphasize relationship building and service quality.
                - For Compliance or Regulatory domains, emphasize accuracy, legal obligations, and risk awareness.
                - For Insurance or Product domains, emphasize product features, benefits, and suitability.
                - For Wealth Management or Tax domains, emphasize planning strategies and client advisory.

                %s
                Ground every module only in the source text. Do not invent facts.
                Reading time must be 1 to 7 minutes and sequenceOrder must be unique.
                """.formatted(domains, outputConverter.getFormat());
    }

    private String userPrompt(String redactedText, List<String> domainNames) {
        return "Domains: %s%nSource:%n%s".formatted(String.join(", ", domainNames), redactedText);
    }

    private MicroModuleSet fallback(String redactedText, List<String> domainNames) {
        String excerpt = redactedText == null ? "" : redactedText.strip();
        if (excerpt.length() > 900) {
            excerpt = excerpt.substring(0, 900).strip();
        }
        String domain = domainNames == null || domainNames.isEmpty() ? "Financial Knowledge" : domainNames.get(0);
        return new MicroModuleSet(
                "Generated training outline for " + domain + ".",
                List.of(
                        new MicroModuleSet.MicroModuleItem(
                                1,
                                domain + " essentials",
                                excerpt.isBlank() ? "Review the uploaded source and identify the main financial learning point." : excerpt,
                                "Focus on the source-backed obligation or product rule.",
                                BigDecimal.valueOf(3.0),
                                List.of()
                        )
                )
        );
    }
}
