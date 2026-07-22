package com.bidforge.exception;

import org.springframework.http.HttpStatus;


// Thrown when a unique value is already taken (like username or email at registration)
// Mapped to HTTP 409 Conflict.

public class DuplicateResourceException extends ApiException {

    public DuplicateResourceException(String message) {
        super(HttpStatus.CONFLICT, "DUPLICATE_RESOURCE", message);
    }
}
