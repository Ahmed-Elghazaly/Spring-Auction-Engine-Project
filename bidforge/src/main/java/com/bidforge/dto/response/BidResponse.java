package com.bidforge.dto.response;

import java.math.BigDecimal;
import java.time.Instant;


// One bid in an auction's bid list
// for Sealed bids before a sealed auction closes, the service only selects the viewer's own bids

public record BidResponse(
        Long id,
        Long auctionId,
        String bidderUsername,
        BigDecimal amount,
        Instant createdAt
) {
}
