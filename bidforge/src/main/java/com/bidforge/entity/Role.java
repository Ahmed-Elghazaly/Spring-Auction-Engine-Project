package com.bidforge.entity;

import com.bidforge.entity.enums.RoleName;
import jakarta.persistence.*;


// security role, exactly two rows exist, inserted at startup by the seeder:
// ROLE_USER and ROLE_ADMIN. Users are linked to roles through the USER_ROLES join table

@Entity
@Table(name = "ROLES")
public class Role {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true, length = 20)
    private RoleName name;


    protected Role() {
    }

    public Role(RoleName name) {
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public RoleName getName() {
        return name;
    }
}
