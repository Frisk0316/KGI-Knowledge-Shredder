package com.kgi.shredder.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "query_log")
public class QueryLog {
    @Id
    @GeneratedValue
    @Column(name = "query_id")
    private UUID queryId;

    @Column(name = "trainer_id", nullable = false)
    private String trainerId;

    @Column(name = "query_text", nullable = false, columnDefinition = "TEXT")
    private String queryText;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "retrieved_chunks", nullable = false, columnDefinition = "jsonb")
    private List<Map<String, Object>> retrievedChunks;

    @Column(name = "token_count_in")
    private Integer tokenCountIn;

    @Column(name = "token_count_out")
    private Integer tokenCountOut;

    @Column(name = "latency_ms")
    private Integer latencyMs;

    @Column(name = "model_used")
    private String modelUsed;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    protected QueryLog() {
    }

    public QueryLog(String trainerId, String queryText, List<Map<String, Object>> retrievedChunks, Integer latencyMs, String modelUsed) {
        this.trainerId = trainerId;
        this.queryText = queryText;
        this.retrievedChunks = retrievedChunks;
        this.latencyMs = latencyMs;
        this.modelUsed = modelUsed;
    }

    public QueryLog(String trainerId, String queryText, List<Map<String, Object>> retrievedChunks,
                    Integer latencyMs, String modelUsed, Integer tokenCountIn, Integer tokenCountOut) {
        this(trainerId, queryText, retrievedChunks, latencyMs, modelUsed);
        this.tokenCountIn = tokenCountIn;
        this.tokenCountOut = tokenCountOut;
    }
}
