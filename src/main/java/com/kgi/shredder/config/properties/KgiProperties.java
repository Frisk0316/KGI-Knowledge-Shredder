package com.kgi.shredder.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "kgi")
public record KgiProperties(
        Ai ai,
        Checkpoint checkpoint,
        Chunking chunking,
        Async async,
        Security security,
        Retention retention
) {
    public record Ai(String embeddingModel, int embeddingDimensions, int ragTopKDefault) {
    }

    public record Checkpoint(boolean enabled, double minScoreToPass, String modelA, String modelB) {
    }

    public record Chunking(int chunkSizeTokens) {
    }

    public record Async(int corePoolSize, int maxPoolSize) {
    }

    public record Security(String devTrainerId, String issuerUri, String jwkSetUri, String audience, String trainerClaim) {
    }

    public record Retention(int softDeleteRetentionDays) {
    }
}
