package com.bidforge.repository;

import com.bidforge.entity.AuctionResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

// written once for each closed auction
public interface AuctionResultRepository extends JpaRepository<AuctionResult, Long> {

    Optional<AuctionResult> findByAuctionId(Long auctionId);

    // Auctions a user won, used for "/api/auctions/won"
    Page<AuctionResult> findByWinnerUsername(String username, Pageable pageable);
}
