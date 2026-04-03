package com.platform.auth.service;

import com.platform.auth.domain.AuditLog;
import com.platform.auth.domain.User;
import com.platform.auth.dto.*;
import com.platform.auth.exception.EmailAlreadyExistsException;
import com.platform.auth.repository.AuditLogRepository;
import com.platform.auth.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository     userRepository;
    private final AuditLogRepository auditLogRepository;
    private final PasswordEncoder    passwordEncoder;
    private final JwtService         jwtService;

    public AuthService(UserRepository userRepository,
                       AuditLogRepository auditLogRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userRepository     = userRepository;
        this.auditLogRepository = auditLogRepository;
        this.passwordEncoder    = passwordEncoder;
        this.jwtService         = jwtService;
    }

    public AuthResponse register(RegisterRequest req) {
        if (userRepository.findByEmail(req.getEmail()).isPresent()) {
            throw new EmailAlreadyExistsException(req.getEmail());
        }
        User user = new User(
                req.getEmail(),
                passwordEncoder.encode(req.getPassword()),
                req.getRole()
        );
        userRepository.save(user);
        log.info("Registered new user email={} role={}", user.getEmail(), user.getRole());
        String token = jwtService.generateToken(user);
        return new AuthResponse(token, user.getId(), user.getRole().name());
    }

    public AuthResponse login(LoginRequest req) {
        User user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        auditLogRepository.save(new AuditLog(user.getId(), user.getEmail(), "LOGIN"));
        log.info("Login success email={}", user.getEmail());

        String token = jwtService.generateToken(user);
        return new AuthResponse(token, user.getId(), user.getRole().name());
    }

    public void logout(String token) {
        try {
            String userId = jwtService.extractUserId(token);
            auditLogRepository.save(new AuditLog(userId, "", "LOGOUT"));
            log.info("Logout userId={}", userId);
        } catch (Exception e) {
            log.warn("Logout called with invalid token: {}", e.getMessage());
        }
    }

    public UserProfileResponse getProfile(String token) {
        String userId = jwtService.extractUserId(token);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
        return new UserProfileResponse(user.getId(), user.getEmail(), user.getRole().name());
    }
}
