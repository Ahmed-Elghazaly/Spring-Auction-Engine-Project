package com.bidforge.exception;

import org.springframework.http.HttpStatus;


// General purpose business rules violation that does not have or need a more specific exception class
// like endTime must be after startTime or minimumIncrement required for English auctions
// Mapped to HTTP 400.

public class BusinessRuleException extends ApiException {

    public BusinessRuleException(String message) {
        super(HttpStatus.BAD_REQUEST, "BUSINESS_RULE_VIOLATION", message);
    }
}
