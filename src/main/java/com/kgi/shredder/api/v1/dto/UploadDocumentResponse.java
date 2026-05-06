package com.kgi.shredder.api.v1.dto;

import java.util.UUID;

public record UploadDocumentResponse(UUID docId, UUID versionId, String previewText) {
}
