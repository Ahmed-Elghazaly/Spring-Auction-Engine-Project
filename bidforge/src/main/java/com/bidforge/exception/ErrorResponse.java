package com.bidforge.exception;

import java.time.Instant;
import java.util.List;

// The single JSON error shape returned by every failing request

/* example
 * {
 *   "timestamp":  "2026-07-18T13:00:00Z",
 *   "status":     422,
 *   "error":      "BID_TOO_LOW",
 *   "message":    "Bid must be at least 130.00 ...",
 *   "path":       "/api/auctions/5/bids",
 *   "fieldErrors": [ { "field": "title", "message": "must not be blank" } ]
 * }
 */

// fieldErrors only appears for DTO validation failures, it is null otherwise

public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        List<FieldValidationError> fieldErrors
) {

    // One invalid DTO field, which field and what is wrong with it
    public record FieldValidationError(String field, String message) {
    }

    // static method to create errors without field details and automatically supply time instant as now
    public static ErrorResponse of(int status, String error, String message, String path) {
        return new ErrorResponse(Instant.now(), status, error, message, path, null);
    }


    // converts this error object to a JSON string by ourselves
    // used only by the two security handlers (RestAuthEntryPoint missing/invalid JWT, RestAccessDeniedHandler forbidden URL)
    // bec run before the request reaches Spring MVC and at that point the normal JSON machinery is not involved, so we write the conversion method ourselves
    public String toJson() {
        return "{\"timestamp\":\"" + timestamp + "\","
                + "\"status\":" + status + ","
                + "\"error\":\"" + escape(error) + "\","
                + "\"message\":\"" + escape(message) + "\","
                + "\"path\":\"" + escape(path) + "\"}";
    }

    private static String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
