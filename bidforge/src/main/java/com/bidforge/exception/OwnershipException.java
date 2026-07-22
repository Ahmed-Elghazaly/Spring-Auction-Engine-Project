package com.bidforge.exception;

import org.springframework.http.HttpStatus;


// Thrown when an authenticated user tries to act on a resource that belongs to someone else
// like editing another seller's auction
// Mapped to HTTP 403 Forbidden.

public class OwnershipException extends ApiException {

    public OwnershipException(String message) {
        super(HttpStatus.FORBIDDEN, "NOT_RESOURCE_OWNER", message);
    }
}
