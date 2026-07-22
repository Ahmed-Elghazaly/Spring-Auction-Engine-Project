package com.bidforge.dto.request;

import com.bidforge.entity.enums.AuctionCategory;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;


// Body of "PUT /api/auctions/{id}"
// full replacement of the editable fields, allowed only while the auction is SCHEDULED and only for its owner
// No auctionType field bec the format cannot change after creation
// No status field bec lifecycle changes happen only through the dedicated open/close/cancel endpoints

public record UpdateAuctionRequest(

        @NotBlank(message = "title is required")
        @Size(min = 3, max = 100, message = "title must be 3-100 characters")
        String title,

        @Size(max = 2000, message = "description must be at most 2000 characters")
        String description,

        @NotNull(message = "category is required")
        AuctionCategory category,

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
