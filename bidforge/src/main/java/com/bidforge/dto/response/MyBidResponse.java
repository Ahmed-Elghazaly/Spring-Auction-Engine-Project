package com.bidforge.dto.response;

import com.bidforge.entity.enums.AuctionStatus;
import com.bidforge.entity.enums.AuctionType;

import java.math.BigDecimal;
import java.time.Instant;


// used for "/api/bids/my"
// a user's own bid with auction's details
// currentlyWinning is only for ENGLISH OPEN auctions
// It is null for sealed auctions bec nobody knows until closing
// and null for finished auctions where we check the auction's result instead
// the mapper sets it

public record MyBidResponse(
        Long id,
        Long auctionId,
        String auctionTitle,
        AuctionType auctionType,
        AuctionStatus auctionStatus,
        BigDecimal amount,
        Instant createdAt,
        Boolean currentlyWinning
) {
}
