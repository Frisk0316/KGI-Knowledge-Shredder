package com.kgi.shredder.api.v1.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record RagQueryRequest(
        @NotBlank String query,
        @JsonProperty("domain_ids") List<Long> domainIds,
        @JsonProperty("top_k") Integer topK
) {
}
