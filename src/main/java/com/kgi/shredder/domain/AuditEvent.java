package com.kgi.shredder.domain;

import com.kgi.shredder.domain.enums.EventType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "audit_events")
public class AuditEvent {
    @Id
    @GeneratedValue
    @Column(name = "event_id")
    private UUID eventId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private EventType eventType;

    @Column(name = "entity_id", nullable = false)
    private String entityId;

    @Column(name = "trainer_id")
    private String trainerId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "event_payload", columnDefinition = "jsonb")
    private Map<String, ?> eventPayload;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    protected AuditEvent() {
    }

    public AuditEvent(EventType eventType, String entityId, String trainerId, Map<String, ?> eventPayload) {
        this.eventType = eventType;
        this.entityId = entityId;
        this.trainerId = trainerId;
        this.eventPayload = eventPayload;
    }

    public UUID getEventId() {
        return eventId;
    }

    public EventType getEventType() {
        return eventType;
    }

    public String getEntityId() {
        return entityId;
    }

    public String getTrainerId() {
        return trainerId;
    }

    public Map<String, ?> getEventPayload() {
        return eventPayload;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
