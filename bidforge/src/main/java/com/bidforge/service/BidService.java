package com.bidforge.service;

import com.bidforge.dto.request.PlaceBidRequest;
import com.bidforge.dto.response.BidResponse;
import com.bidforge.dto.response.MyBidResponse;
import com.bidforge.entity.Auction;
import com.bidforge.entity.Bid;
import com.bidforge.entity.User;
import com.bidforge.entity.enums.AuctionStatus;
import com.bidforge.entity.enums.AuctionType;
import com.bidforge.entity.enums.AuditAction;
import com.bidforge.exception.*;
import com.bidforge.mapper.BidMapper;
import com.bidforge.repository.AuctionRepository;
import com.bidforge.repository.BidRepository;
import com.bidforge.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

// uses locking to handle racing
@Service
public class BidService {

    private final BidRepository bidRepository;
    private final AuctionRepository auctionRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public BidService(BidRepository bidRepository,
                      AuctionRepository auctionRepository,
                      UserRepository userRepository,
                      AuditService auditService) {
        this.bidRepository = bidRepository;
        this.auctionRepository = auctionRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }


    // Places a bid, all inside one locked transaction
    @Transactional
    public BidResponse placeBid(String username, Long auctionId, PlaceBidRequest request) {
        // lock the auction row before reading anything, from here till commit, no other bid on this same auction can happen
        Auction auction = auctionRepository.findByIdForUpdate(auctionId)
                .orElseThrow(() -> ResourceNotFoundException.of("Auction", auctionId));


        User bidder = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User '" + username + "' was not found."));

        // the seller must not bid on their own auction.
        if (auction.getSeller().getId().equals(bidder.getId())) {
            throw new SellerCannotBidException("You cannot bid on your own auction.");
        }

        // the auction must be live
        if (auction.getStatus() != AuctionStatus.OPEN) {
            throw new InvalidAuctionStateException(
                    "Bids are only accepted while the auction is OPEN, this auction is "
                            + auction.getStatus() + ".");
        }
        if (Instant.now().isAfter(auction.getEndTime())) {
            throw new InvalidAuctionStateException(
                    "This auction's end time has passed; it is about to close and no longer accepts bids.");
        }

        // the format-specific pricing rules
        BigDecimal amount = request.amount();
        if (auction.getAuctionType() == AuctionType.ENGLISH) {
            validateEnglishAmount(auction, amount);
        } else {
            validateSealedRules(auction, bidder, amount);
        }

        // save the bid
        Bid bid = bidRepository.save(new Bid(amount, auction, bidder));

        // an accepted ENGLISH bid becomes the new visible highest, in the same transaction as the insert
        if (auction.getAuctionType() == AuctionType.ENGLISH) {
            auction.setCurrentHighestBid(amount);
        }

        auditService.record(username, AuditAction.BID_PLACED, "AUCTION", auction.getId(),
                "bid " + amount + (auction.getAuctionType() == AuctionType.SEALED_BID ? " (sealed)" : ""));

        return BidMapper.toResponse(bid);
    }

    // for English bids, first bid must reach the starting price
    // each later bid must beat the highest by at least the minimum increment
    private void validateEnglishAmount(Auction auction, BigDecimal amount) {
        BigDecimal highest = auction.getCurrentHighestBid();
        BigDecimal minimumAcceptable = (highest == null)
                ? auction.getStartingPrice()
                : highest.add(auction.getMinIncrement());
        if (amount.compareTo(minimumAcceptable) < 0) {
            String reason = (highest == null)
                    ? "the first bid must be at least the starting price " + auction.getStartingPrice()
                    : "it must beat the current highest bid " + highest
                      + " by the minimum increment " + auction.getMinIncrement();
            throw new BidTooLowException(
                    "Bid of " + amount + " is too low: " + reason
                            + ". Minimum acceptable bid: " + minimumAcceptable + ".");
        }
    }


    // one bid per user only
    private void validateSealedRules(Auction auction, User bidder, BigDecimal amount) {
        // this check is safe from racing bec because the auction row is locked so
        // any two parallel of the same user on the same auction occur after the other here and the second fails
        if (bidRepository.existsByAuctionIdAndBidderUsername(auction.getId(), bidder.getUsername())) {
            throw new DuplicateSealedBidException(
                    "You already placed your sealed bid on this auction — sealed bids are final "
                            + "and cannot be changed.");
        }
        if (amount.compareTo(auction.getStartingPrice()) < 0) {
            throw new BidTooLowException(
                    "Bid of " + amount + " is too low: sealed bids must be at least the starting price "
                            + auction.getStartingPrice() + ".");
        }
    }


    // Bid list of an auction, taking in account the diff visibility rules for English and Sealed auctions
    // viewerUsername is the authenticated user, or null for anonymous browsing
    @Transactional(readOnly = true)
    public Page<BidResponse> getBidsForAuction(String viewerUsername, Long auctionId, Pageable pageable) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> ResourceNotFoundException.of("Auction", auctionId));

        // return all bids for any English or Closed auction
        boolean revealed = auction.getAuctionType() == AuctionType.ENGLISH
                || auction.getStatus() == AuctionStatus.CLOSED;

        if (revealed) {
            return bidRepository.findByAuctionId(auctionId, pageable).map(BidMapper::toResponse);
        }
        // don't return anything for a Sealed auction viewed by an anonymous user
        if (viewerUsername == null) {
            return Page.empty(pageable);
        }
        // return logged-in user bids only for a sealed auction
        return bidRepository.findByAuctionIdAndBidderUsername(auctionId, viewerUsername, pageable)
                .map(BidMapper::toResponse);
    }


    // All bids of the logged-in user, newest first by default
    @Transactional(readOnly = true)
    public Page<MyBidResponse> myBids(String username, Pageable pageable) {
        return bidRepository.findByBidderUsername(username, pageable)
                .map(BidMapper::toMyBidResponse);
    }
}
