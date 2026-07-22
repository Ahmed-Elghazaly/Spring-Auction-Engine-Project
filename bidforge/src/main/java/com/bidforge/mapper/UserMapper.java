package com.bidforge.mapper;

import com.bidforge.dto.response.UserResponse;
import com.bidforge.entity.User;


// Converts User entities into UserResponse DTOs

public final class UserMapper {

    private UserMapper() {
    }

    public static UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.isEnabled(),
                user.getCreatedAt(),
                user.getRoles().stream()
                        .map(role -> role.getName().name())
                        .sorted()
                        .toList());
    }
}
