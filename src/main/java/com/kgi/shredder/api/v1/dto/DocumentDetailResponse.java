package com.kgi.shredder.api.v1.dto;

import java.util.List;
import java.util.UUID;

public record DocumentDetailResponse(
        UUID docId,
        String originalFilename,
        String contentHash,
        UUID currentVersionId,
        String processingStatus,
        List<DomainResponse> domains,
        List<VersionSummary> versions
) {
    public record VersionSummary(UUID versionId, int versionNumber, String processingStatus, String parserName) {
    }
}
