package com.bidforge.exception;

import org.springframework.http.HttpStatus;


// Thrown when the seller tries to bid on their own auction
// bec bidding on someone's own auction would let him push the price up artificially
// Mapped to HTTP 409 Conflict
public class SellerCannotBidException extends ApiException {

    public SellerCannotBidException(String message) {
        super(HttpStatus.CONFLICT, "SELLER_CANNOT_BID", message);
    }
}
