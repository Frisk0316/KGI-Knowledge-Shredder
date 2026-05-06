package com.kgi.shredder.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.kgi.shredder.config.properties.KgiProperties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class OpenAiEmbeddingGateway implements EmbeddingGateway {
    private final RestClient restClient;
    private final KgiProperties properties;
    private final String apiKey;

    public OpenAiEmbeddingGateway(
            RestClient.Builder restClientBuilder,
            KgiProperties properties,
            @Value("${spring.ai.openai.api-key:}") String apiKey
    ) {
        this.restClient = restClientBuilder.baseUrl("https://api.openai.com").build();
        this.properties = properties;
        this.apiKey = apiKey;
    }

    @Override
    public float[] embed(String text) {
        if (apiKey == null || apiKey.isBlank()) {
            return deterministicEmbedding(text);
        }
        try {
            JsonNode response = restClient.post()
                    .uri("/v1/embeddings")
                    .header("Authorization", "Bearer " + apiKey)
                    .body(Map.of(
                            "model", properties.ai().embeddingModel(),
                            "input", text
                    ))
                    .retrieve()
                    .body(JsonNode.class);
            JsonNode embedding = response.path("data").path(0).path("embedding");
            float[] values = new float[embedding.size()];
            for (int i = 0; i < embedding.size(); i++) {
                values[i] = (float) embedding.get(i).asDouble();
            }
            return values;
        } catch (Exception ignored) {
            return deterministicEmbedding(text);
        }
    }

    private float[] deterministicEmbedding(String text) {
        int dimensions = properties.ai().embeddingDimensions();
        float[] vector = new float[dimensions];
        byte[] seed = sha256(text == null ? "" : text);
        for (int i = 0; i < dimensions; i++) {
            int value = seed[i % seed.length] & 0xff;
            vector[i] = (value - 128) / 128.0f;
        }
        return vector;
    }

    private byte[] sha256(String text) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ex) {
            return HexFormat.of().parseHex("000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f");
        }
    }
}
