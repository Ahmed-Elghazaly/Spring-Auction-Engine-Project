package com.bidforge.exception;

import org.springframework.http.HttpStatus;


// Thrown when a bid amount does not satisfy the auction's pricing rules
// like below the starting price, or not larger than the current highest bid by the minimum amount
// Mapped to HTTP 422 Unprocessable Entity
public class BidTooLowException extends ApiException {

    public BidTooLowException(String message) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, "BID_TOO_LOW", message);
    }
}
