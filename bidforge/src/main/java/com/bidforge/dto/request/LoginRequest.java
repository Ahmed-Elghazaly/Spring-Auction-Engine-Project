package com.bidforge.dto.request;

import jakarta.validation.constraints.NotBlank;

// Body of "POST /api/auth/login"
public record LoginRequest(

        @NotBlank(message = "username is required")
        String username,

        @NotBlank(message = "password is required")
        String password
) {
}
