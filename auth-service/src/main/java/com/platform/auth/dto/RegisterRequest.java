package com.platform.auth.dto;

import com.platform.auth.domain.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class RegisterRequest {

    @Email @NotBlank
    private String email;

    @NotBlank
    private String password;

    @NotNull
    private Role role;

    public RegisterRequest() {}

    public RegisterRequest(String email, String password, Role role) {
        this.email    = email;
        this.password = password;
        this.role     = role;
    }

    public String getEmail()    { return email; }
    public String getPassword() { return password; }
    public Role   getRole()     { return role; }

    public void setEmail(String email)       { this.email = email; }
    public void setPassword(String password) { this.password = password; }
    public void setRole(Role role)           { this.role = role; }
}
