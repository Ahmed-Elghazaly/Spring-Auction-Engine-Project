package com.bidforge.dto.request;

import com.bidforge.entity.enums.AuctionCategory;
import com.bidforge.entity.enums.AuctionType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;


// Body of "POST /api/auctions"
// single field rules are checked here as annotations
// Rules that span multiple fields like "endTime after startTime" and "minIncrement amound required for English bids
// but forbidden for Sealed bids" are checked in AuctionService
public record CreateAuctionRequest(

        @NotBlank(message = "title is required")
        @Size(min = 3, max = 100, message = "title must be 3-100 characters")
        String title,

        @Size(max = 2000, message = "description must be at most 2000 characters")
        String description,

        @NotNull(message = "category is required")
        AuctionCategory category,

        @NotNull(message = "auctionType is required (ENGLISH or SEALED_BID)")
        AuctionType auctionType,

        @NotNull(message = "startingPrice is required")
        @DecimalMin(value = "0.01", message = "startingPrice must be at least 0.01")
        @Digits(integer = 17, fraction = 2, message = "startingPrice must have at most 2 decimal places")
        BigDecimal startingPrice,

        @DecimalMin(value = "0.01", message = "minIncrement must be at least 0.01")
        @Digits(integer = 17, fraction = 2, message = "minIncrement must have at most 2 decimal places")
        BigDecimal minIncrement,

        @NotNull(message = "startTime is required")
        @Future(message = "startTime must be in the future")
        Instant startTime,

        @NotNull(message = "endTime is required")
        @Future(message = "endTime must be in the future")
        Instant endTime
) {
}
