package com.bidforge.exception;

import org.springframework.http.HttpStatus;


// Thrown when a user who already placed their single sealed bid tries to place another one on the same auction
// Mapped to HTTP 409 Conflict.

public class DuplicateSealedBidException extends ApiException {

    public DuplicateSealedBidException(String message) {
        super(HttpStatus.CONFLICT, "SEALED_BID_ALREADY_PLACED", message);
    }
}
