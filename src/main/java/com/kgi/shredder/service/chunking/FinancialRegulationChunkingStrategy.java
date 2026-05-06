package com.kgi.shredder.service.chunking;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class FinancialRegulationChunkingStrategy implements ChunkingStrategy {
    private static final Pattern ARTICLE_BOUNDARY = Pattern.compile("(?m)(?=^\\s*(第[一二三四五六七八九十百零0-9]+條|Article\\s+\\d+))");

    @Override
    public boolean supports(String classification) {
        return "REGULATORY".equalsIgnoreCase(classification);
    }

    @Override
    public List<DocumentChunkCandidate> chunk(String text) {
        List<String> parts = splitOnArticleBoundaries(text);
        List<DocumentChunkCandidate> chunks = new ArrayList<>();
        for (int i = 0; i < parts.size(); i++) {
            chunks.add(new DocumentChunkCandidate(i, parts.get(i), "financial_reg"));
        }
        return chunks;
    }

    private List<String> splitOnArticleBoundaries(String text) {
        Matcher matcher = ARTICLE_BOUNDARY.matcher(text);
        List<Integer> starts = new ArrayList<>();
        while (matcher.find()) {
            starts.add(matcher.start());
        }
        if (starts.isEmpty()) {
            return List.of(text.strip());
        }

        List<String> chunks = new ArrayList<>();
        for (int i = 0; i < starts.size(); i++) {
            int start = starts.get(i);
            int end = (i + 1 < starts.size()) ? starts.get(i + 1) : text.length();
            String chunk = text.substring(start, end).strip();
            if (!chunk.isBlank()) {
                chunks.add(chunk);
            }
        }
        return chunks;
    }
}
