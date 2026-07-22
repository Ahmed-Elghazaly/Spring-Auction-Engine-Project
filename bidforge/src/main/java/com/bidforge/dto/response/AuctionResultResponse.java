package com.bidforge.dto.response;

import java.math.BigDecimal;
import java.time.Instant;


// Outcome of a CLOSED auction
// winnerUsername, finalPrice are null when the auction closed without any bids

public record AuctionResultResponse(
        String winnerUsername,
        BigDecimal finalPrice,
        Instant closedAt
) {
}
