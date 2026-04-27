package com.platform.desktop.api.dto;

public class UserProfileResponse {
    public String userId;
    public String email;
    public String role;

    public UserProfileResponse() {}

    public Role roleEnum() {
        return Role.fromString(role);
    }
}
