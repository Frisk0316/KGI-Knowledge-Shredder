package com.kgi.shredder.domain.enums;

public enum ProcessingStatus {
    PENDING,
    REDACTING,
    CHUNKING,
    VECTORIZING,
    READY,
    FAILED
}
