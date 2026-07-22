package com.bidforge.service;

import com.bidforge.dto.request.LoginRequest;
import com.bidforge.dto.request.RegisterRequest;
import com.bidforge.dto.response.AuthResponse;
import com.bidforge.dto.response.UserResponse;
import com.bidforge.entity.Role;
import com.bidforge.entity.User;
import com.bidforge.entity.enums.AuditAction;
import com.bidforge.entity.enums.RoleName;
import com.bidforge.exception.DuplicateResourceException;
import com.bidforge.mapper.UserMapper;
import com.bidforge.repository.RoleRepository;
import com.bidforge.repository.UserRepository;
import com.bidforge.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


// for Registration and login, the only two operations that exist before a user has a token

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final AuditService auditService;

    public AuthService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtService jwtService,
                       AuditService auditService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.auditService = auditService;
    }


    // Takes Request DTO, Creates a new account and Returns UserResponse DTO
    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new DuplicateResourceException("Username '" + request.username() + "' is already taken.");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Email '" + request.email() + "' is already registered.");
        }

        User user = new User(
                request.username(),
                request.email(),
                // we store only the BCrypt hash of the password
                passwordEncoder.encode(request.password()),
                request.firstName(),
                request.lastName());

        // everyone who registers is a normal user, Admin accounts exist only through seed data, there is no API to become one
        Role userRole = roleRepository.findByName(RoleName.ROLE_USER)
                .orElseThrow(() -> new IllegalStateException("ROLE_USER missing — seeding did not run"));
        user.addRole(userRole);

        User saved = userRepository.save(user);
        auditService.record(saved.getUsername(), AuditAction.USER_REGISTERED, "USER", saved.getId(), null);
        return UserMapper.toResponse(saved);
    }


    // Verifies credentials and issues a JWT token
    // authenticationManager.authenticate(...) runs the real check, it loads the account via CustomUserDetailsService and compares password hashes
    // Wrong credentials throws BadCredentialsException and disabled account throws DisabledException
    // Both are AuthenticationExceptions and become 401 JSON GlobalExceptionHandler
    // this method only continues on success
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String token = jwtService.generateToken(userDetails);

        return new AuthResponse(
                token,
                "Bearer",
                jwtService.getExpirationMs(),
                userDetails.getUsername(),
                userDetails.getAuthorities().stream().map(Object::toString).sorted().toList());
    }
}
