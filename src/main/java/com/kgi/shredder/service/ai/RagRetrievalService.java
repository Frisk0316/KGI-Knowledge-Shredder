package com.kgi.shredder.service.ai;

import java.util.List;

public interface RagRetrievalService {
    RagAnswer answer(String trainerId, String query, List<Long> domainIds, int topK);

    record RagAnswer(String answer, List<RagSource> sources) {
    }

    record RagSource(String chunkId, double similarityScore, String excerpt) {
    }
}
