package com.kgi.shredder.api.v1;

import com.kgi.shredder.api.v1.dto.AuditEventResponse;
import com.kgi.shredder.api.v1.dto.IncidentResponse;
import com.kgi.shredder.api.v1.dto.IncidentUpdateRequest;
import com.kgi.shredder.domain.AuditEvent;
import com.kgi.shredder.domain.DocumentIncident;
import com.kgi.shredder.domain.enums.EventType;
import com.kgi.shredder.exception.ResourceNotFoundException;
import com.kgi.shredder.repository.AuditEventRepository;
import com.kgi.shredder.repository.DocumentIncidentRepository;
import com.kgi.shredder.service.audit.AuditService;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {
    private final AuditEventRepository auditEventRepository;
    private final DocumentIncidentRepository documentIncidentRepository;
    private final AuditService auditService;

    public AdminController(
            AuditEventRepository auditEventRepository,
            DocumentIncidentRepository documentIncidentRepository,
            AuditService auditService
    ) {
        this.auditEventRepository = auditEventRepository;
        this.documentIncidentRepository = documentIncidentRepository;
        this.auditService = auditService;
    }

    @GetMapping("/audit-events")
    public Page<AuditEventResponse> auditEvents(Pageable pageable) {
        return auditEventRepository.findAll(pageable).map(this::toAuditResponse);
    }

    @GetMapping("/incidents")
    public Page<IncidentResponse> incidents(@RequestParam(required = false) Boolean resolved, Pageable pageable) {
        Page<DocumentIncident> incidents = resolved == null
                ? documentIncidentRepository.findAll(pageable)
                : documentIncidentRepository.findByResolved(resolved, pageable);
        return incidents.map(this::toIncidentResponse);
    }

    @PatchMapping("/incidents/{incidentId}")
    public IncidentResponse updateIncident(@PathVariable UUID incidentId, @RequestBody IncidentUpdateRequest request) {
        DocumentIncident incident = documentIncidentRepository.findById(incidentId)
                .orElseThrow(() -> new ResourceNotFoundException("Incident not found."));
        incident.setResolved(request.resolved());
        DocumentIncident saved = documentIncidentRepository.save(incident);
        auditService.log(EventType.INCIDENT_RESOLVED, incidentId.toString(), null, Map.of("resolved", request.resolved()));
        return toIncidentResponse(saved);
    }

    private AuditEventResponse toAuditResponse(AuditEvent event) {
        return new AuditEventResponse(
                event.getEventId(),
                event.getEventType(),
                event.getEntityId(),
                event.getTrainerId(),
                event.getEventPayload(),
                event.getCreatedAt()
        );
    }

    private IncidentResponse toIncidentResponse(DocumentIncident incident) {
        return new IncidentResponse(
                incident.getIncidentId(),
                incident.getIncidentType(),
                incident.getSeverity(),
                incident.isResolved(),
                incident.getDetails(),
                incident.getCreatedAt()
        );
    }
}
