package com.platform.desktop.api.dto;

public class AuthResponse {
    public String token;
    public String userId;
    public String role;

    public AuthResponse() {}

    public Role roleEnum() {
        return Role.fromString(role);
    }
}
