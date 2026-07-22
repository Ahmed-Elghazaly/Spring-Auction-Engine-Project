package com.bidforge.entity;

import com.bidforge.entity.enums.AuditAction;
import jakarta.persistence.*;

import java.time.Instant;

// One line of the log: WHO did WHAT to WHICH record WHEN, Insert only and never updated.

@Entity
@Table(name = "AUDIT_EVENTS", indexes = {
        // speed up admin filtering log by the affected record and sorting by time.
        @Index(name = "idx_audit_entity", columnList = "entity_type, entity_id"),
        @Index(name = "idx_audit_created", columnList = "created_at")
})
public class AuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Username of who acted, or "SYSTEM" for the scheduler, WHO
    @Column(nullable = false, length = 50)
    private String actor;

    // WHAT
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AuditAction action;

    // Kind of record affected: "USER" or "AUCTION"
    // WHICH
    @Column(name = "entity_type", nullable = false, length = 30)
    private String entityType;

    // Primary key (ID) of the affected record
    // WHICH
    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    // short text description of event
    @Column(length = 500)
    private String details;

    // WHEN
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    protected AuditEvent() {
    }

    public AuditEvent(String actor, AuditAction action, String entityType, Long entityId, String details) {
        this.actor = actor;
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.details = details;
    }

    public Long getId() {
        return id;
    }

    public String getActor() {
        return actor;
    }

    public AuditAction getAction() {
        return action;
    }

    public String getEntityType() {
        return entityType;
    }

    public Long getEntityId() {
        return entityId;
    }

    public String getDetails() {
        return details;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
