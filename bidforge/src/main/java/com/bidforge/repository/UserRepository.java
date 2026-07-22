package com.bidforge.repository;

import com.bidforge.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;


public interface UserRepository extends JpaRepository<User, Long> {

    // Used by login and by the JWT filter to load the authenticated account
    Optional<User> findByUsername(String username);

    // Registration uniqueness check
    boolean existsByUsername(String username);

    // Registration uniqueness check
    boolean existsByEmail(String email);
}
