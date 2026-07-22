package com.bidforge.dto.response;

import java.util.List;


// Response of a successful login, the JWT plus everything a client needs to use it
// The client sends the token back on every request as the header "Authorization: Bearer <token>"

public record AuthResponse(
        String token,
        String tokenType,      // always "Bearer"
        long expiresInMs,      // token lifetime from now, in milliseconds
        String username,
        List<String> roles
) {
}
