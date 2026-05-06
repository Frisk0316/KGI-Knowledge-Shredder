package com.kgi.shredder.api.v1.dto;

import java.util.List;

public record RagQueryResponse(String answer, List<RagSourceResponse> sources) {
    public record RagSourceResponse(String chunkId, double similarityScore, String excerpt) {
    }
}
