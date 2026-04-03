package com.platform.auth.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document("users")
public class User {

    @Id
    private String id;

    @Indexed(unique = true)
    private String email;

    private String passwordHash;
    private Role role;
    private Instant createdAt = Instant.now();

    public User() {}

    public User(String email, String passwordHash, Role role) {
        this.email        = email;
        this.passwordHash = passwordHash;
        this.role         = role;
    }

    public String getId()           { return id; }
    public void   setId(String id)  { this.id = id; }

    public String getEmail()              { return email; }
    public void   setEmail(String email)  { this.email = email; }

    public String getPasswordHash()                   { return passwordHash; }
    public void   setPasswordHash(String passwordHash){ this.passwordHash = passwordHash; }

    public Role getRole()           { return role; }
    public void setRole(Role role)  { this.role = role; }

    public Instant getCreatedAt()               { return createdAt; }
    public void    setCreatedAt(Instant createdAt){ this.createdAt = createdAt; }
}
