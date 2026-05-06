package com.kgi.shredder.service.chunking;

public record DocumentChunkCandidate(int index, String text, String strategy) {
}
