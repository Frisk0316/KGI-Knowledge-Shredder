package com.kgi.shredder.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class OllamaStage1RedactionService implements Stage1RedactionService {
    private static final Pattern EMAIL = Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");
    private static final Pattern PHONE = Pattern.compile("(?<!\\d)(?:\\+?\\d[\\d\\-\\s()]{7,}\\d)(?!\\d)");
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String model;

    public OllamaStage1RedactionService(
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper,
            @Value("${spring.ai.ollama.base-url:http://localhost:11434}") String baseUrl,
            @Value("${spring.ai.ollama.chat.options.model:qwen2.5:7b}") String model
    ) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
        this.objectMapper = objectMapper;
        this.model = model;
    }

    @Override
    public Stage1Result redactAndClassify(String rawText) {
        try {
            String content = restClient.post()
                    .uri("/api/generate")
                    .body(Map.of(
                            "model", model,
                            "stream", false,
                            "format", "json",
                            "prompt", prompt(rawText)
                    ))
                    .retrieve()
                    .body(JsonNode.class)
                    .path("response")
                    .asText();
            JsonNode json = objectMapper.readTree(content);
            String redactedText = json.path("redactedText").asText();
            String classification = json.path("classification").asText("OTHER");
            boolean piiFound = json.path("piiFound").asBoolean(false);
            double compressionRatio = json.path("compressionRatio").asDouble(compressionRatio(rawText, redactedText));
            if (!redactedText.isBlank()) {
                return new Stage1Result(redactedText, normalizeClassification(classification), piiFound, compressionRatio);
            }
        } catch (Exception ignored) {
            // Local deterministic fallback keeps development and tests independent from a running Ollama daemon.
        }
        return fallback(rawText);
    }

    private String prompt(String rawText) {
        return """
                You are a local financial-document safety processor.
                Return JSON only with keys redactedText, classification, piiFound, compressionRatio.
                classification must be one of REGULATORY, PRODUCT_SPEC, PROCESS_GUIDE, OTHER.
                Redact personal emails, phone numbers, national IDs, addresses, and customer names.
                Preserve financial facts, article numbers, product constraints, and compliance obligations.

                Document:
                %s
                """.formatted(rawText);
    }

    private Stage1Result fallback(String rawText) {
        String redacted = EMAIL.matcher(rawText).replaceAll("[REDACTED_EMAIL]");
        redacted = PHONE.matcher(redacted).replaceAll("[REDACTED_PHONE]");
        boolean piiFound = !redacted.equals(rawText);
        return new Stage1Result(redacted, classify(rawText), piiFound, compressionRatio(rawText, redacted));
    }

    private String classify(String text) {
        String normalized = text.toLowerCase();
        if (normalized.contains("第") && normalized.contains("條") || normalized.contains("article")) {
            return "REGULATORY";
        }
        if (normalized.contains("premium") || normalized.contains("policy") || normalized.contains("product")) {
            return "PRODUCT_SPEC";
        }
        if (normalized.contains("process") || normalized.contains("workflow") || normalized.contains("步驟")) {
            return "PROCESS_GUIDE";
        }
        return "OTHER";
    }

    private String normalizeClassification(String classification) {
        return switch (classification == null ? "" : classification.toUpperCase()) {
            case "REGULATORY", "PRODUCT_SPEC", "PROCESS_GUIDE" -> classification.toUpperCase();
            default -> "OTHER";
        };
    }

    private double compressionRatio(String rawText, String redactedText) {
        if (rawText == null || rawText.isBlank()) {
            return 1.0;
        }
        return Math.max(0.01, Math.min(1.0, (double) redactedText.length() / rawText.length()));
    }
}
