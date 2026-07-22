package com.bidforge.service;

import com.bidforge.dto.response.AuctionSummaryResponse;
import com.bidforge.dto.response.UserResponse;
import com.bidforge.entity.User;
import com.bidforge.entity.enums.AuctionCategory;
import com.bidforge.entity.enums.AuctionStatus;
import com.bidforge.entity.enums.AuctionType;
import com.bidforge.entity.enums.AuditAction;
import com.bidforge.exception.BusinessRuleException;
import com.bidforge.exception.ResourceNotFoundException;
import com.bidforge.mapper.AuctionMapper;
import com.bidforge.mapper.UserMapper;
import com.bidforge.repository.AuctionRepository;
import com.bidforge.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// two security layers
// the URL rule in SecurityConfig "/api/admin/**" hasRole ADMIN
// the class-level @PreAuthorize to protect if someone exposed these methods from another endpoint by mistake

@Service
@PreAuthorize("hasRole('ADMIN')")
public class AdminService {

    private final UserRepository userRepository;
    private final AuctionRepository auctionRepository;
    private final AuditService auditService;

    public AdminService(UserRepository userRepository,
                        AuctionRepository auctionRepository,
                        AuditService auditService) {
        this.userRepository = userRepository;
        this.auctionRepository = auctionRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> listUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(UserMapper::toResponse);
    }


    // Disabling takes effect immediately, the JWT filter rechecks the flag on every request
    // so an already issued token stops working directly on the next call
    @Transactional
    public UserResponse updateUserStatus(String actorUsername, Long userId, boolean enabled) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ResourceNotFoundException.of("User", userId));

        if (user.getUsername().equals(actorUsername)) {
            throw new BusinessRuleException("You cannot change the status of your own account.");
        }

        user.setEnabled(enabled);
        auditService.record(actorUsername, AuditAction.USER_STATUS_CHANGED, "USER", user.getId(),
                enabled ? "account enabled" : "account disabled");
        return UserMapper.toResponse(user);
    }

    @Transactional(readOnly = true)
    public Page<AuctionSummaryResponse> searchAuctions(AuctionStatus status, AuctionType type,
                                                       AuctionCategory category, String q,
                                                       String sellerUsername, Pageable pageable) {
        String query = (q == null || q.isBlank()) ? null : q.trim();
        String seller = (sellerUsername == null || sellerUsername.isBlank()) ? null : sellerUsername.trim();
        return auctionRepository.adminSearch(status, type, category, query, seller, pageable)
                .map(AuctionMapper::toSummary);
    }
}
