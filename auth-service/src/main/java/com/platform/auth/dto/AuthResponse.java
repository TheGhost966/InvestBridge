package com.platform.auth.dto;

public class AuthResponse {

    private String token;
    private String userId;
    private String role;

    public AuthResponse(String token, String userId, String role) {
        this.token  = token;
        this.userId = userId;
        this.role   = role;
    }

    public String getToken()  { return token; }
    public String getUserId() { return userId; }
    public String getRole()   { return role; }
}
