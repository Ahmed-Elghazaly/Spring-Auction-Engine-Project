package com.bidforge.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.List;


// We convert exceptions to HTTP responses here

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);


    // for all business exceptions like 404 not found, 409 conflicts, 422 bid too low, 403 ownership, 400 rule violations
    // Each subclass of ApiException already carries its HTTP status and error code
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException ex, HttpServletRequest request) {
        ErrorResponse body = ErrorResponse.of(
                ex.getStatus().value(), ex.getErrorCode(), ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(ex.getStatus()).body(body);
    }


    // Bean Validation failures on request DTOs
    // Spring throws this automatically
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex,
                                                          HttpServletRequest request) {
        List<ErrorResponse.FieldValidationError> fields = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ErrorResponse.FieldValidationError(fe.getField(), fe.getDefaultMessage()))
                .toList();
        ErrorResponse body = new ErrorResponse(Instant.now(), HttpStatus.BAD_REQUEST.value(),
                "VALIDATION_FAILED", "Request validation failed.", request.getRequestURI(), fields);
        return ResponseEntity.badRequest().body(body);
    }


    // for unreadable request body like broken JSON, or a value that cannot be converted like auctionType "Ahmed" which is not a valid enum constant
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException ex,
                                                          HttpServletRequest request) {
        ErrorResponse body = ErrorResponse.of(HttpStatus.BAD_REQUEST.value(), "MALFORMED_REQUEST",
                "Request body is missing, not valid JSON, or contains an invalid value "
                        + "(check enum fields like auctionType/category and number/date formats)",
                request.getRequestURI());
        return ResponseEntity.badRequest().body(body);
    }


    // URL parameter of the wrong type, like "/api/auctions/abc" bec abc is not a number
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
                                                            HttpServletRequest request) {
        ErrorResponse body = ErrorResponse.of(HttpStatus.BAD_REQUEST.value(), "MALFORMED_REQUEST",
                "Parameter '" + ex.getName() + "' has an invalid value.", request.getRequestURI());
        return ResponseEntity.badRequest().body(body);
    }


    // Thrown by Spring Security when method level security in MVC layer like @PreAuthorize("hasRole('ADMIN')")}) rejects an authenticated user
    // Requests rejected earlier, at the URL level, are handled by RestAccessDeniedHandler in the security package instead
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex,
                                                            HttpServletRequest request) {
        ErrorResponse body = ErrorResponse.of(HttpStatus.FORBIDDEN.value(), "FORBIDDEN",
                "You do not have permission to perform this action.", request.getRequestURI());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }


    // Login failures thrown by the AuthenticationManager inside AuthService.login like wrong credentials (BadCredentialsException)
    // or a disabled account (DisabledException)
    // Both return 401
    @ExceptionHandler(org.springframework.security.core.AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(
            org.springframework.security.core.AuthenticationException ex, HttpServletRequest request) {
        boolean disabled = ex instanceof org.springframework.security.authentication.DisabledException;
        ErrorResponse body = ErrorResponse.of(HttpStatus.UNAUTHORIZED.value(),
                disabled ? "ACCOUNT_DISABLED" : "INVALID_CREDENTIALS",
                disabled ? "This account has been disabled by an administrator."
                        : "Invalid username or password.",
                request.getRequestURI());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }


    // Unknown URL, error 404
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResource(NoResourceFoundException ex,
                                                          HttpServletRequest request) {
        ErrorResponse body = ErrorResponse.of(HttpStatus.NOT_FOUND.value(), "RESOURCE_NOT_FOUND",
                "No endpoint exists at this path.", request.getRequestURI());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }


    // correct URL but wrong HTTP verb like DELETE on /api/auctions
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex,
                                                                  HttpServletRequest request) {
        ErrorResponse body = ErrorResponse.of(HttpStatus.METHOD_NOT_ALLOWED.value(), "METHOD_NOT_ALLOWED",
                "HTTP method " + ex.getMethod() + " is not supported at this path.", request.getRequestURI());
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(body);
    }


    // Safety net for anything unexpected
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error handling {} {}", request.getMethod(), request.getRequestURI(), ex);
        ErrorResponse body = ErrorResponse.of(HttpStatus.INTERNAL_SERVER_ERROR.value(), "INTERNAL_ERROR",
                "An unexpected error occurred. Please try again or contact support.", request.getRequestURI());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
