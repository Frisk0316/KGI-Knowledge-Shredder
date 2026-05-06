package com.kgi.shredder.api.v1.dto;

import com.kgi.shredder.domain.enums.EventType;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public record AuditEventResponse(
        UUID eventId,
        EventType eventType,
        String entityId,
        String trainerId,
        Map<String, ?> payload,
        OffsetDateTime createdAt
) {
}
