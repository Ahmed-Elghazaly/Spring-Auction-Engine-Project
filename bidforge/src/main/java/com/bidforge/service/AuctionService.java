package com.bidforge.service;

import com.bidforge.dto.request.CreateAuctionRequest;
import com.bidforge.dto.request.UpdateAuctionRequest;
import com.bidforge.dto.response.AuctionResponse;
import com.bidforge.dto.response.AuctionSummaryResponse;
import com.bidforge.entity.Auction;
import com.bidforge.entity.AuctionResult;
import com.bidforge.entity.Bid;
import com.bidforge.entity.User;
import com.bidforge.entity.enums.*;
import com.bidforge.exception.BusinessRuleException;
import com.bidforge.exception.InvalidAuctionStateException;
import com.bidforge.exception.OwnershipException;
import com.bidforge.exception.ResourceNotFoundException;
import com.bidforge.mapper.AuctionMapper;
import com.bidforge.repository.AuctionRepository;
import com.bidforge.repository.AuctionResultRepository;
import com.bidforge.repository.BidRepository;
import com.bidforge.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;


// All business logic around the auction lifecycle except bidding and closing, BidService and closing build on top of this

@Service
public class AuctionService {


    private static final String AUDIT_ENTITY = "AUCTION";

    private final AuctionRepository auctionRepository;
    private final UserRepository userRepository;
    private final BidRepository bidRepository;
    private final AuctionResultRepository auctionResultRepository;
    private final AuditService auditService;

    public AuctionService(AuctionRepository auctionRepository,
                          UserRepository userRepository,
                          BidRepository bidRepository,
                          AuctionResultRepository auctionResultRepository,
                          AuditService auditService) {
        this.auctionRepository = auctionRepository;
        this.userRepository = userRepository;
        this.bidRepository = bidRepository;
        this.auctionResultRepository = auctionResultRepository;
        this.auditService = auditService;
    }


    // Read/Create

    // Creates an auction in SCHEDULED state
    @Transactional
    public AuctionResponse create(String username, CreateAuctionRequest request) {
        validateTimes(request.startTime(), request.endTime());
        validateIncrementRule(request.auctionType(), request.minIncrement());

        User seller = loadUser(username);
        Auction auction = new Auction(
                request.title(),
                request.description(),
                request.category(),
                request.auctionType(),
                request.startingPrice(),
                // if client sent minIncrement for a sealed auction it is set to null
                request.auctionType() == AuctionType.SEALED_BID ? null : request.minIncrement(),
                request.startTime(),
                request.endTime(),
                seller);

        Auction saved = auctionRepository.save(auction);
        auditService.record(username, AuditAction.AUCTION_CREATED, AUDIT_ENTITY, saved.getId(),
                "\"" + saved.getTitle() + "\" (" + saved.getAuctionType() + ")");
        return AuctionMapper.toResponse(saved);
    }

    // Detailed view, once CLOSED it also carries the outcome like winner and final price
    @Transactional(readOnly = true)
    public AuctionResponse getById(Long id) {
        Auction auction = findAuction(id);
        AuctionResult result = (auction.getStatus() == AuctionStatus.CLOSED)
                ? auctionResultRepository.findByAuctionId(id).orElse(null)
                : null;
        return AuctionMapper.toResponse(auction, result);
    }


    // browse with optional filters + pagination
    @Transactional(readOnly = true)
    public Page<AuctionSummaryResponse> search(AuctionStatus status, AuctionType type,
                                               AuctionCategory category, String q, Pageable pageable) {
        String query = (q == null || q.isBlank()) ? null : q.trim();
        return auctionRepository.search(status, type, category, query, pageable)
                .map(AuctionMapper::toSummary);
    }


    // Auctions created by the logged-in user
    @Transactional(readOnly = true)
    public Page<AuctionSummaryResponse> myAuctions(String username, Pageable pageable) {
        return auctionRepository.findBySellerUsername(username, pageable)
                .map(AuctionMapper::toSummary);
    }


    // Update/lifecycle transitions


    // Full edit by owner only for SCHEDULED auctions only
    @Transactional
    public AuctionResponse update(String username, Long id, UpdateAuctionRequest request) {
        Auction auction = lockAuction(id);
        requireOwner(auction, username, "Only the auction owner can edit it.");
        requireStatus(auction, AuctionStatus.SCHEDULED, "Only SCHEDULED auctions can be edited");

        validateTimes(request.startTime(), request.endTime());
        validateIncrementRule(auction.getAuctionType(), request.minIncrement());

        auction.setTitle(request.title());
        auction.setDescription(request.description());
        auction.setCategory(request.category());
        auction.setStartingPrice(request.startingPrice());
        auction.setMinIncrement(
                auction.getAuctionType() == AuctionType.SEALED_BID ? null : request.minIncrement());
        auction.setStartTime(request.startTime());
        auction.setEndTime(request.endTime());

        auditService.record(username, AuditAction.AUCTION_UPDATED, AUDIT_ENTITY, auction.getId(), null);
        return AuctionMapper.toResponse(auction);
    }


    // transition from SCHEDULED to OPEN
    // Called by the owner/admin to start early
    @Transactional
    public AuctionResponse open(String actorUsername, Long id) {
        Auction auction = lockAuction(id);
        requireOwnerOrAdmin(auction, actorUsername, "Only the auction owner or an admin can open it.");
        requireStatus(auction, AuctionStatus.SCHEDULED, "Only SCHEDULED auctions can be opened");

        auction.setStatus(AuctionStatus.OPEN);
        auditService.record(actorUsername, AuditAction.AUCTION_OPENED, AUDIT_ENTITY, auction.getId(), null);
        return AuctionMapper.toResponse(auction);
    }


    // Scheduler variant of open, in this case actor is "SYSTEM"
    @Transactional
    public void openBySystem(Long id) {
        Auction auction = lockAuction(id);
        if (auction.getStatus() != AuctionStatus.SCHEDULED) {
            return;
        }
        auction.setStatus(AuctionStatus.OPEN);
        auditService.record(AuditService.SYSTEM_ACTOR, AuditAction.AUCTION_OPENED, AUDIT_ENTITY,
                auction.getId(), "auto-opened (start time reached)");
    }


    // changes SCHEDULED to CANCELLED by owner or admin and OPEN to CANCELLED by admin only
    @Transactional
    public AuctionResponse cancel(String actorUsername, Long id) {
        Auction auction = lockAuction(id);
        User actor = loadUser(actorUsername);
        boolean admin = isAdmin(actor);
        boolean owner = auction.getSeller().getUsername().equals(actorUsername);

        if (!owner && !admin) {
            throw new OwnershipException("Only the auction owner or an admin can cancel it.");
        }
        switch (auction.getStatus()) {
            case SCHEDULED -> {
            }
            case OPEN -> {
                if (!admin) {
                    throw new InvalidAuctionStateException(
                            "This auction is already OPEN, only an administrator can cancel a live auction.");
                }
            }
            default -> throw new InvalidAuctionStateException(
                    "A " + auction.getStatus() + " auction cannot be cancelled.");
        }

        auction.setStatus(AuctionStatus.CANCELLED);
        auditService.record(actorUsername, AuditAction.AUCTION_CANCELLED, AUDIT_ENTITY, auction.getId(),
                admin && !owner ? "cancelled by administrator" : null);
        return AuctionMapper.toResponse(auction);
    }


    // changes OPEN to CLOSED with winner determination
    // Called by the owner/admin (close early) and by the scheduler when endTime passes
    @Transactional
    public AuctionResponse close(String actorUsername, Long id) {
        Auction auction = lockAuction(id);
        requireOwnerOrAdmin(auction, actorUsername, "Only the auction owner or an admin can close it.");
        requireStatus(auction, AuctionStatus.OPEN, "Only OPEN auctions can be closed");

        AuctionResult result = doClose(auction, actorUsername);
        return AuctionMapper.toResponse(auction, result);
    }


    // Scheduler variant of close
    @Transactional
    public void closeBySystem(Long id) {
        Auction auction = lockAuction(id);
        if (auction.getStatus() != AuctionStatus.OPEN) {
            return;
        }
        doClose(auction, AuditService.SYSTEM_ACTOR);
    }


    // shared function by owner/admin/scheduler for closing work which runs under the caller's lock and pick the winner
    private AuctionResult doClose(Auction auction, String actor) {
        Bid winningBid = bidRepository
                .findFirstByAuctionIdOrderByAmountDescCreatedAtAsc(auction.getId())
                .orElse(null);

        AuctionResult result = new AuctionResult(
                auction,
                winningBid == null ? null : winningBid.getBidder(),
                winningBid,
                winningBid == null ? null : winningBid.getAmount(),
                Instant.now());
        auctionResultRepository.save(result);

        auction.setStatus(AuctionStatus.CLOSED);

        auditService.record(actor, AuditAction.AUCTION_CLOSED, AUDIT_ENTITY, auction.getId(),
                winningBid == null ? "closed with no bids" : "closed");
        if (winningBid != null) {
            auditService.record(actor, AuditAction.WINNER_SELECTED, AUDIT_ENTITY, auction.getId(),
                    "winner " + winningBid.getBidder().getUsername() + " at " + winningBid.getAmount());
        }
        return result;
    }


    // Auctions the logged-in user won
    @Transactional(readOnly = true)
    public Page<AuctionSummaryResponse> wonAuctions(String username, Pageable pageable) {
        return auctionResultRepository.findByWinnerUsername(username, pageable)
                .map(result -> AuctionMapper.toSummary(result.getAuction()));
    }


    Auction findAuction(Long id) {
        return auctionRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Auction", id));
    }


    Auction lockAuction(Long id) {
        return auctionRepository.findByIdForUpdate(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Auction", id));
    }

    User loadUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User '" + username + "' was not found."));
    }

    boolean isAdmin(User user) {
        return user.getRoles().stream().anyMatch(role -> role.getName() == RoleName.ROLE_ADMIN);
    }

    private void requireOwner(Auction auction, String username, String message) {
        if (!auction.getSeller().getUsername().equals(username)) {
            throw new OwnershipException(message);
        }
    }

    private void requireOwnerOrAdmin(Auction auction, String username, String message) {
        if (!auction.getSeller().getUsername().equals(username) && !isAdmin(loadUser(username))) {
            throw new OwnershipException(message);
        }
    }

    private void requireStatus(Auction auction, AuctionStatus expected, String messagePrefix) {
        if (auction.getStatus() != expected) {
            throw new InvalidAuctionStateException(
                    messagePrefix + " — this auction is " + auction.getStatus() + ".");
        }
    }


    private void validateTimes(Instant startTime, Instant endTime) {
        if (!endTime.isAfter(startTime)) {
            throw new BusinessRuleException("endTime must be after startTime.");
        }
    }


    private void validateIncrementRule(AuctionType type, BigDecimal minIncrement) {
        if (type == AuctionType.ENGLISH && minIncrement == null) {
            throw new BusinessRuleException("minIncrement is required for ENGLISH auctions.");
        }
        if (type == AuctionType.SEALED_BID && minIncrement != null) {
            throw new BusinessRuleException(
                    "minIncrement must not be set for SEALED_BID auctions (bids only need to reach the starting price).");
        }
    }
}
