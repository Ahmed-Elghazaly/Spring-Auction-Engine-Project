package com.bidforge.repository;

import com.bidforge.entity.Role;
import com.bidforge.entity.enums.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {

    // used by seeder and during registration
    Optional<Role> findByName(RoleName name);
}
