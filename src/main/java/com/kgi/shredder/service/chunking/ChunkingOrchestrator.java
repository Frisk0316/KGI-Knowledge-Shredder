package com.kgi.shredder.service.chunking;

import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ChunkingOrchestrator {
    private final List<ChunkingStrategy> strategies;

    public ChunkingOrchestrator(List<ChunkingStrategy> strategies) {
        this.strategies = strategies.stream()
                .sorted(Comparator.comparing(strategy -> strategy instanceof TokenChunkingStrategy))
                .toList();
    }

    public List<DocumentChunkCandidate> chunk(String text, String classification) {
        return strategies.stream()
                .filter(strategy -> strategy.supports(classification))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No chunking strategy available."))
                .chunk(text);
    }
}
