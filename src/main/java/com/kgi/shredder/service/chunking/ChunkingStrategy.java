package com.kgi.shredder.service.chunking;

import java.util.List;

public interface ChunkingStrategy {
    boolean supports(String classification);

    List<DocumentChunkCandidate> chunk(String text);
}
