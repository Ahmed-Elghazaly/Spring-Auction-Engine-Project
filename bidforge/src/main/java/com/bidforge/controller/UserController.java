package com.bidforge.controller;

import com.bidforge.dto.response.UserResponse;
import com.bidforge.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Endpoints for the currently logged-in user account
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }


    // returns 200 and info for currently logged-in user
    // Authentication parameter is injected by Spring, it is the object the JWT filter stored in the SecurityContext for this request
    // authentication.getName() is the username from the token
    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(Authentication authentication) {
        return ResponseEntity.ok(userService.getByUsername(authentication.getName()));
    }
}
