package com.bidforge.dto.request;

import jakarta.validation.constraints.NotNull;


// Body of "PATCH /api/admin/users/{id}/status"
// used by admins to enable or disable an account
public record UpdateUserStatusRequest(

        @NotNull(message = "enabled is required (true or false)")
        Boolean enabled
) {
}
