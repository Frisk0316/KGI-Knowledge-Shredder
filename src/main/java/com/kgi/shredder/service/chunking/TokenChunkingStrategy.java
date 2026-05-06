package com.kgi.shredder.service.chunking;

import com.kgi.shredder.config.properties.KgiProperties;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class TokenChunkingStrategy implements ChunkingStrategy {
    private static final Pattern SENTENCE_BOUNDARY = Pattern.compile("(?<=[。！？；;.!?])\\s*");
    private final int chunkSizeTokens;

    public TokenChunkingStrategy(KgiProperties properties) {
        this.chunkSizeTokens = properties.chunking().chunkSizeTokens();
    }

    @Override
    public boolean supports(String classification) {
        return true;
    }

    @Override
    public List<DocumentChunkCandidate> chunk(String text) {
        List<DocumentChunkCandidate> chunks = new ArrayList<>();
        StringBuilder buffer = new StringBuilder();
        int index = 0;
        for (String sentence : SENTENCE_BOUNDARY.split(text)) {
            if (sentence.isBlank()) {
                continue;
            }
            if (estimatedTokens(buffer + sentence) > chunkSizeTokens && !buffer.isEmpty()) {
                chunks.add(new DocumentChunkCandidate(index++, buffer.toString().strip(), "token"));
                buffer.setLength(0);
            }
            buffer.append(sentence.strip()).append(' ');
        }
        if (!buffer.isEmpty()) {
            chunks.add(new DocumentChunkCandidate(index, buffer.toString().strip(), "token"));
        }
        return chunks;
    }

    private int estimatedTokens(String text) {
        long cjk = text.chars().filter(ch -> Character.UnicodeScript.of(ch) == Character.UnicodeScript.HAN).count();
        int nonCjk = text.replaceAll("[\\p{IsHan}]", "").split("\\s+").length;
        return (int) cjk + nonCjk;
    }
}
