package com.bidforge.dto.response;

import java.time.Instant;
import java.util.List;


// Public representation of a user account
public record UserResponse(
        Long id,
        String username,
        String email,
        String firstName,
        String lastName,
        boolean enabled,
        Instant createdAt,
        List<String> roles
) {
}
