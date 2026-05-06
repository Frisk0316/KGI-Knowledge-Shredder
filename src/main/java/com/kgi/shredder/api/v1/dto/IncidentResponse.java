package com.kgi.shredder.api.v1.dto;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public record IncidentResponse(
        UUID incidentId,
        String incidentType,
        String severity,
        boolean resolved,
        Map<String, ?> details,
        OffsetDateTime createdAt
) {
}
