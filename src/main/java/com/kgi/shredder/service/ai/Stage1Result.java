package com.kgi.shredder.service.ai;

public record Stage1Result(String redactedText, String classification, boolean piiFound, double compressionRatio) {
}
