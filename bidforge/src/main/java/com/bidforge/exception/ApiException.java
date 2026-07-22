package com.bidforge.exception;

import org.springframework.http.HttpStatus;


// Base class for every error that we raise on purpose
// Each subclass fixes the HTTP status code and the errorCode
// all subclasses are converted into the consistent JSON body by GlobalExceptionHandler (via ErrorResponse DTO)
public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String errorCode;

    protected ApiException(HttpStatus status, String errorCode, String message) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
