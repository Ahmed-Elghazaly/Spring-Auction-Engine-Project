package com.bidforge.exception;

import org.springframework.http.HttpStatus;


// Thrown when an action is not allowed in the auction's current status
// like bidding on a CLOSED auction, editing an OPEN auction, or closing an auction twice
// Mapped to HTTP 409 Conflict

public class InvalidAuctionStateException extends ApiException {

    public InvalidAuctionStateException(String message) {
        super(HttpStatus.CONFLICT, "INVALID_AUCTION_STATE", message);
    }
}
