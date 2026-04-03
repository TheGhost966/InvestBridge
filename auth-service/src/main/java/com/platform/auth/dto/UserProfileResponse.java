package com.platform.auth.dto;

public class UserProfileResponse {

    private String userId;
    private String email;
    private String role;

    public UserProfileResponse(String userId, String email, String role) {
        this.userId = userId;
        this.email  = email;
        this.role   = role;
    }

    public String getUserId() { return userId; }
    public String getEmail()  { return email; }
    public String getRole()   { return role; }
}
