package com.kgi.shredder.api.v1;

import com.kgi.shredder.api.v1.dto.RagQueryRequest;
import com.kgi.shredder.api.v1.dto.RagQueryResponse;
import com.kgi.shredder.config.SecurityContextUtil;
import com.kgi.shredder.service.ai.RagRetrievalService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/rag")
public class RagController {
    private final RagRetrievalService ragRetrievalService;

    public RagController(RagRetrievalService ragRetrievalService) {
        this.ragRetrievalService = ragRetrievalService;
    }

    @PostMapping("/query")
    public RagQueryResponse query(@Valid @RequestBody RagQueryRequest request) {
        var answer = ragRetrievalService.answer(
                SecurityContextUtil.currentTrainerId(),
                request.query(),
                request.domainIds(),
                request.topK() == null ? 0 : request.topK()
        );
        return new RagQueryResponse(
                answer.answer(),
                answer.sources().stream()
                        .map(source -> new RagQueryResponse.RagSourceResponse(
                                source.chunkId(),
                                source.similarityScore(),
                                source.excerpt()
                        ))
                        .toList()
        );
    }
}
