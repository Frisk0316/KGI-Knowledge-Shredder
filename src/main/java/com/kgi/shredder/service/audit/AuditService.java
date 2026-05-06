package com.kgi.shredder.service.audit;

import com.kgi.shredder.domain.AuditEvent;
import com.kgi.shredder.domain.enums.EventType;
import com.kgi.shredder.repository.AuditEventRepository;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class AuditService {
    private final AuditEventRepository auditEventRepository;

    public AuditService(AuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }

    public void log(EventType eventType, String entityId, String trainerId, Map<String, ?> payload) {
        auditEventRepository.save(new AuditEvent(eventType, entityId, trainerId, payload));
    }
}
