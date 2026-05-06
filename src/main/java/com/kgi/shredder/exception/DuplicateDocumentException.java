package com.kgi.shredder.exception;

import java.util.UUID;

public class DuplicateDocumentException extends RuntimeException {
    private final UUID existingDocId;

    public DuplicateDocumentException(UUID existingDocId) {
        super("Duplicate document already exists for this trainer.");
        this.existingDocId = existingDocId;
    }

    public UUID getExistingDocId() {
        return existingDocId;
    }
}
