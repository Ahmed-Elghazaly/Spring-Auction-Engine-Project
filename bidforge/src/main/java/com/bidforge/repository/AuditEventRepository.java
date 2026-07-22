package com.bidforge.repository;

import com.bidforge.entity.AuditEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

// read by admins only, inserted via AuditService
public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {

    @Query("""
            select e from AuditEvent e
            where (:entityType is null or e.entityType = :entityType)
              and (:entityId is null or e.entityId = :entityId)
              and (:actor is null or e.actor = :actor)
            """)
    Page<AuditEvent> search(@Param("entityType") String entityType, @Param("entityId") Long entityId, @Param("actor") String actor, Pageable pageable);
}
