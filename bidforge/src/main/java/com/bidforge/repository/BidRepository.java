package com.bidforge.repository;

import com.bidforge.entity.Bid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BidRepository extends JpaRepository<Bid, Long> {


    // all bids of one auction Full bid history of an auction
    // used always for English bids and only after closing for sealed bids
    Page<Bid> findByAuctionId(Long auctionId, Pageable pageable);

    // A user's bids on one auction
    Page<Bid> findByAuctionIdAndBidderUsername(Long auctionId, String username, Pageable pageable);

    // All bids of one user across auctions, user for "/api/bids/my"
    Page<Bid> findByBidderUsername(String username, Pageable pageable);

    // used for sealed bids to check if user has already used his single sealed bid
    boolean existsByAuctionIdAndBidderUsername(Long auctionId, String username);

    // Winner determination, highest amount first, and on equal amounts the earliest submission first
    Optional<Bid> findFirstByAuctionIdOrderByAmountDescCreatedAtAsc(Long auctionId);
}
