package com.kgi.shredder.api.v1.dto;

import java.util.UUID;

public record JobResponse(
        UUID jobId,
        String status,
        String errorMessage,
        Boolean validationPassed,
        String validationOutput
) {
    public JobResponse(UUID jobId, String status, String errorMessage) {
        this(jobId, status, errorMessage, null, null);
    }
}
