package com.bidforge.service;

import com.bidforge.dto.response.AuditEventResponse;
import com.bidforge.entity.AuditEvent;
import com.bidforge.entity.enums.AuditAction;
import com.bidforge.repository.AuditEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


// Writes and reads the audit log

@Service
public class AuditService {

    public static final String SYSTEM_ACTOR = "SYSTEM";

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditEventRepository auditEventRepository;

    public AuditService(AuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }

    // Appends one line to the audit trail, happens inside the caller's transaction
    public void record(String actor, AuditAction action, String entityType, Long entityId, String details) {
        try {
            auditEventRepository.save(new AuditEvent(actor, action, entityType, entityId, details));
        } catch (Exception ex) {
            log.error("Failed to write audit event {} {} {}#{}", actor, action, entityType, entityId, ex);
        }
    }

    // log search for Admins
    @Transactional(readOnly = true)
    public Page<AuditEventResponse> search(String entityType, Long entityId, String actor, Pageable pageable) {
        String type = blankToNull(entityType);
        String actorFilter = blankToNull(actor);
        return auditEventRepository.search(type, entityId, actorFilter, pageable)
                .map(e -> new AuditEventResponse(e.getId(), e.getActor(), e.getAction(),
                        e.getEntityType(), e.getEntityId(), e.getDetails(), e.getCreatedAt()));
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
