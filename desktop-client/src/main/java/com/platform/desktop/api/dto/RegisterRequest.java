package com.platform.desktop.api.dto;

public class RegisterRequest {
    public String email;
    public String password;
    public Role role;

    public RegisterRequest() {}

    public RegisterRequest(String email, String password, Role role) {
        this.email = email;
        this.password = password;
        this.role = role;
    }
}
