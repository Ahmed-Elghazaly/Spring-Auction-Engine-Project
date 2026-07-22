package com.bidforge.dto.response;

import com.bidforge.entity.enums.AuctionCategory;
import com.bidforge.entity.enums.AuctionStatus;
import com.bidforge.entity.enums.AuctionType;

import java.math.BigDecimal;
import java.time.Instant;


 // Full auction detail, result is present only once the auction is CLOSED, null before that so omitted from JSON
public record AuctionResponse(
        Long id,
        String title,
        String description,
        AuctionCategory category,
        AuctionType auctionType,
        AuctionStatus status,
        BigDecimal startingPrice,
        BigDecimal minIncrement,
        BigDecimal currentHighestBid,
        Instant startTime,
        Instant endTime,
        String sellerUsername,
        Instant createdAt,
        Instant updatedAt,
        AuctionResultResponse result
) {
}
