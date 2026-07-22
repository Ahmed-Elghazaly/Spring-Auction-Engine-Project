package com.bidforge.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;


// for both normal users and administrators
// Auctions and bids reference the user from their side with seller/ bidder fields


@Entity
@Table(name = "USERS")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    // Login name, unique
    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, unique = true, length = 150)
    private String email;


    // BCrypt hash of the password not the plain text
    @Column(nullable = false, length = 100)
    private String password;

    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 50)
    private String lastName;


    // switch used by administrators, when false Spring Security rejects the login and the JWT filter rejects even existing tokens
    @Column(nullable = false)
    private boolean enabled = true;


    // Set only once when the row is first saved, never updated afterwards
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // Many-to-Many bec a user can hold several roles and a role belongs to many users
    // The join table USER_ROLES holds (user_id, role_id) pairs.
    // We use Eager fetching bec the set holds at most two rows and is needed on every authenticated request to compute authorities
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "USER_ROLES", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles = new HashSet<>();


    // Runs automatically just before the first INSERT
    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    protected User() {
    }

    public User(String username, String email, String password, String firstName, String lastName) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Set<Role> getRoles() {
        return roles;
    }

    public void addRole(Role role) {
        this.roles.add(role);
    }
}
