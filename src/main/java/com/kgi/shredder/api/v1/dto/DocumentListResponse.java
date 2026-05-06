package com.kgi.shredder.api.v1.dto;

import java.util.UUID;

public record DocumentListResponse(UUID docId, String originalFilename, UUID currentVersionId) {
}
