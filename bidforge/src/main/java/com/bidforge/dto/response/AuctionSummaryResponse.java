package com.bidforge.dto.response;

import com.bidforge.entity.enums.AuctionCategory;
import com.bidforge.entity.enums.AuctionStatus;
import com.bidforge.entity.enums.AuctionType;

import java.math.BigDecimal;
import java.time.Instant;


// Compact auction card used in browse, mine, admin lists
// currentHighestBid is null for Sealed bid auctions so they don't appear in JSON response

public record AuctionSummaryResponse(
        Long id,
        String title,
        AuctionCategory category,
        AuctionType auctionType,
        AuctionStatus status,
        BigDecimal startingPrice,
        BigDecimal currentHighestBid,
        Instant startTime,
        Instant endTime,
        String sellerUsername
) {
}
