package com.bidforge.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;


// Body of "POST /api/auth/register"
// The Bean Validation annotations run automatically because the controller parameter is marked @Valid
// failures return one 400 response listing every wrong field via GlobalExceptionHandler

public record RegisterRequest(

        @NotBlank(message = "username is required")
        @Size(min = 3, max = 50, message = "username must be 3-50 characters")
        String username,

        @NotBlank(message = "email is required")
        @Email(message = "email must be a valid email address")
        @Size(max = 150, message = "email must be at most 150 characters")
        String email,

        @NotBlank(message = "password is required")
        @Size(min = 8, max = 72, message = "password must be 8-72 characters")
        String password,

        @NotBlank(message = "firstName is required")
        @Size(max = 50, message = "firstName must be at most 50 characters")
        String firstName,

        @NotBlank(message = "lastName is required")
        @Size(max = 50, message = "lastName must be at most 50 characters")
        String lastName
) {
}
