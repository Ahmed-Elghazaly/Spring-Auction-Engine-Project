package com.bidforge.controller;

import com.bidforge.dto.request.UpdateUserStatusRequest;
import com.bidforge.dto.response.AuctionSummaryResponse;
import com.bidforge.dto.response.AuditEventResponse;
import com.bidforge.dto.response.UserResponse;
import com.bidforge.entity.enums.AuctionCategory;
import com.bidforge.entity.enums.AuctionStatus;
import com.bidforge.entity.enums.AuctionType;
import com.bidforge.service.AdminService;
import com.bidforge.service.AuditService;
import com.bidforge.util.Paging;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Set;


// Every URL under /api/admin/** already requires ROLE_ADMIN at the security-filter level
// a normal user gets 403 before anything here runs, the service layer re-checks with @PreAuthorize

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;
    private final AuditService auditService;

    public AdminController(AdminService adminService, AuditService auditService) {
        this.adminService = adminService;
        this.auditService = auditService;
    }


    @GetMapping("/users")
    public ResponseEntity<Page<UserResponse>> listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort) {
        return ResponseEntity.ok(adminService.listUsers(
                Paging.of(page, size, sort, Set.of("createdAt", "username", "email"))));
    }


    @PatchMapping("/users/{id}/status")
    public ResponseEntity<UserResponse> updateUserStatus(@PathVariable Long id,
                                                         @Valid @RequestBody UpdateUserStatusRequest request,
                                                         Authentication authentication) {
        return ResponseEntity.ok(
                adminService.updateUserStatus(authentication.getName(), id, request.enabled()));
    }

    @GetMapping("/auctions")
    public ResponseEntity<Page<AuctionSummaryResponse>> listAuctions(
            @RequestParam(required = false) AuctionStatus status,
            @RequestParam(required = false) AuctionType type,
            @RequestParam(required = false) AuctionCategory category,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String seller,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort) {
        return ResponseEntity.ok(adminService.searchAuctions(status, type, category, q, seller,
                Paging.of(page, size, sort,
                        Set.of("createdAt", "startTime", "endTime", "startingPrice", "currentHighestBid"))));
    }

    
    @GetMapping("/audit-events")
    public ResponseEntity<Page<AuditEventResponse>> listAuditEvents(
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) Long entityId,
            @RequestParam(required = false) String actor,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort) {
        return ResponseEntity.ok(auditService.search(entityType, entityId, actor,
                Paging.of(page, size, sort, Set.of("createdAt"))));
    }
}
