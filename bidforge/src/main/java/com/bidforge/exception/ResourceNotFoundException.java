package com.bidforge.exception;

import org.springframework.http.HttpStatus;


// Thrown when a requested row does not exist
// Mapped to HTTP 404 Not Found.

public class ResourceNotFoundException extends ApiException {

    public ResourceNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", message);
    }

    public static ResourceNotFoundException of(String entityName, Object id) {
        return new ResourceNotFoundException(entityName + " with id " + id + " was not found.");
    }
}
