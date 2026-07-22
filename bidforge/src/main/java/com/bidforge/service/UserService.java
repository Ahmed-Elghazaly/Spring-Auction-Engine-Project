package com.bidforge.service;

import com.bidforge.dto.response.UserResponse;
import com.bidforge.entity.User;
import com.bidforge.exception.ResourceNotFoundException;
import com.bidforge.mapper.UserMapper;
import com.bidforge.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// used for "GET /users/me"
@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }


    // Profile of the currently authenticated user.
    @Transactional(readOnly = true)
    public UserResponse getByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User '" + username + "' was not found."));
        return UserMapper.toResponse(user);
    }
}
