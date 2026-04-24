package com.platform.auth.service;

import com.platform.auth.audit.AuditEvent;
import com.platform.auth.audit.AuditLogWriter;
import com.platform.auth.domain.User;
import com.platform.auth.dto.*;
import com.platform.auth.exception.EmailAlreadyExistsException;
import com.platform.auth.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository  userRepository;
    private final ObjectProvider<AuditLogWriter> auditWriter;
    private final PasswordEncoder passwordEncoder;
    private final JwtService      jwtService;

    /**
     * {@link AuditLogWriter} is injected via {@link ObjectProvider} so the
     * service still starts when the JDBC layer is absent (e.g. unit tests
     * that do not boot PostgreSQL). Missing audit storage degrades to a
     * log warning — we never fail a login because the audit store is down.
     */
    public AuthService(UserRepository userRepository,
                       ObjectProvider<AuditLogWriter> auditWriter,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userRepository  = userRepository;
        this.auditWriter     = auditWriter;
        this.passwordEncoder = passwordEncoder;
        this.jwtService      = jwtService;
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
        writeAudit(user.getId(), user.getEmail(), AuditEvent.EVENT_REGISTER);
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

        writeAudit(user.getId(), user.getEmail(), AuditEvent.EVENT_LOGIN);
        log.info("Login success email={}", user.getEmail());

        String token = jwtService.generateToken(user);
        return new AuthResponse(token, user.getId(), user.getRole().name());
    }

    public void logout(String token) {
        try {
            String userId = jwtService.extractUserId(token);
            writeAudit(userId, null, AuditEvent.EVENT_LOGOUT);
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

    // ── helpers ────────────────────────────────────────────────────────────

    private void writeAudit(String userId, String email, String event) {
        AuditLogWriter w = auditWriter.getIfAvailable();
        if (w == null) {
            log.debug("Audit writer not wired — skipping {} event for userId={}", event, userId);
            return;
        }
        try {
            w.log(new AuditEvent(userId, email, event));
        } catch (RuntimeException ex) {
            log.warn("Audit write failed event={} userId={}: {}", event, userId, ex.getMessage());
        }
    }
}
