package com.kgi.shredder.domain.enums;

public enum GenerationJobStatus {
    QUEUED,
    STAGE1_REDACTING,
    STAGE2_GENERATING,
    VALIDATING,
    VECTORIZING,
    COMPLETED,
    FAILED
}
