package com.kgi.shredder.service.ai;

public interface EmbeddingGateway {
    float[] embed(String text);
}
