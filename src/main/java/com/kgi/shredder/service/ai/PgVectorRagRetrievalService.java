package com.kgi.shredder.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.kgi.shredder.config.properties.KgiProperties;
import com.kgi.shredder.domain.QueryLog;
import com.kgi.shredder.repository.QueryLogRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class PgVectorRagRetrievalService implements RagRetrievalService {
    private final EmbeddingGateway embeddingGateway;
    private final JdbcTemplate jdbcTemplate;
    private final QueryLogRepository queryLogRepository;
    private final KgiProperties properties;
    private final RestClient openAiClient;
    private final String apiKey;
    private final String model;

    public PgVectorRagRetrievalService(
            EmbeddingGateway embeddingGateway,
            JdbcTemplate jdbcTemplate,
            QueryLogRepository queryLogRepository,
            KgiProperties properties,
            RestClient.Builder restClientBuilder,
            @Value("${spring.ai.openai.api-key:}") String apiKey,
            @Value("${spring.ai.openai.chat.options.model:gpt-4o}") String model
    ) {
        this.embeddingGateway = embeddingGateway;
        this.jdbcTemplate = jdbcTemplate;
        this.queryLogRepository = queryLogRepository;
        this.properties = properties;
        this.openAiClient = restClientBuilder.baseUrl("https://api.openai.com").build();
        this.apiKey = apiKey;
        this.model = model;
    }

    @Override
    public RagAnswer answer(String trainerId, String query, List<Long> domainIds, int topK) {
        long started = System.currentTimeMillis();
        int normalizedTopK = topK <= 0 ? properties.ai().ragTopKDefault() : Math.min(topK, 20);
        String vector = DocumentVectorizationService.vectorLiteral(embeddingGateway.embed(query));
        List<RagSource> sources = search(trainerId, domainIds == null ? List.of() : domainIds, normalizedTopK, vector);
        SynthesisResult synthesis = synthesizeAnswer(query, sources);
        int latency = (int) (System.currentTimeMillis() - started);
        queryLogRepository.save(new QueryLog(
                trainerId,
                query,
                sources.stream()
                        .map(source -> Map.<String, Object>of(
                                "chunk_id", source.chunkId(),
                                "similarity_score", source.similarityScore(),
                                "excerpt", source.excerpt()
                        ))
                        .toList(),
                latency,
                synthesis.modelUsed,
                synthesis.tokenCountIn,
                synthesis.tokenCountOut
        ));
        return new RagAnswer(synthesis.answer, sources);
    }

    private List<RagSource> search(String trainerId, List<Long> domainIds, int topK, String vector) {
        StringBuilder sql = new StringBuilder("""
                SELECT metadata->>'chunk_id' AS chunk_id,
                       content,
                       1 - (embedding <=> ?::vector) AS similarity_score
                FROM document_embeddings
                WHERE metadata->>'trainer_id' = ?
                """);
        List<Object> args = new ArrayList<>();
        args.add(vector);
        args.add(trainerId);
        if (!domainIds.isEmpty()) {
            sql.append(" AND EXISTS (SELECT 1 FROM jsonb_array_elements_text(metadata->'domain_ids') AS domain(value) WHERE domain.value IN (");
            for (int i = 0; i < domainIds.size(); i++) {
                if (i > 0) {
                    sql.append(", ");
                }
                sql.append("?");
                args.add(domainIds.get(i).toString());
            }
            sql.append("))");
        }
        sql.append(" ORDER BY embedding <=> ?::vector LIMIT ?");
        args.add(vector);
        args.add(topK);

        return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> new RagSource(
                rs.getString("chunk_id"),
                rs.getDouble("similarity_score"),
                rs.getString("content")
        ), args.toArray());
    }

    private SynthesisResult synthesizeAnswer(String query, List<RagSource> sources) {
        if (sources.isEmpty()) {
            return new SynthesisResult("No tenant-scoped source chunks were found for: " + query, "none", null, null);
        }
        StringBuilder context = new StringBuilder();
        for (int i = 0; i < sources.size(); i++) {
            context.append("[Source ").append(i + 1).append("] ").append(sources.get(i).excerpt()).append("\n\n");
        }
        if (apiKey == null || apiKey.isBlank()) {
            return fallbackSynthesis(sources);
        }
        try {
            JsonNode response = openAiClient.post()
                    .uri("/v1/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .body(Map.of(
                            "model", model,
                            "messages", List.of(
                                    Map.of("role", "system", "content",
                                            "You are a financial knowledge assistant. Answer the user's question based ONLY on the provided source chunks. " +
                                            "Be concise, accurate, and cite source numbers when possible. If the sources do not contain enough information, say so."),
                                    Map.of("role", "user", "content",
                                            "Question: " + query + "\n\nSources:\n" + context)
                            )
                    ))
                    .retrieve()
                    .body(JsonNode.class);
            String answer = response.path("choices").path(0).path("message").path("content").asText();
            JsonNode usage = response.path("usage");
            Integer tokensIn = usage.has("prompt_tokens") ? usage.get("prompt_tokens").asInt() : null;
            Integer tokensOut = usage.has("completion_tokens") ? usage.get("completion_tokens").asInt() : null;
            return new SynthesisResult(answer, model, tokensIn, tokensOut);
        } catch (Exception ignored) {
            return fallbackSynthesis(sources);
        }
    }

    private SynthesisResult fallbackSynthesis(List<RagSource> sources) {
        StringBuilder answer = new StringBuilder("Based on the retrieved source chunks: ");
        for (int i = 0; i < sources.size(); i++) {
            if (i > 0) {
                answer.append(" ");
            }
            String excerpt = sources.get(i).excerpt();
            answer.append(excerpt.length() > 240 ? excerpt.substring(0, 240).strip() : excerpt);
        }
        return new SynthesisResult(answer.toString(), "fallback", null, null);
    }

    private record SynthesisResult(String answer, String modelUsed, Integer tokenCountIn, Integer tokenCountOut) {
    }
}
