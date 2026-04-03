package com.platform.auth.controller;

import com.platform.auth.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * STUB — returns wrong status codes intentionally.
 * Tests will FAIL (RED phase).
 * Replace body in GREEN phase.
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        // STUB: returns 200 instead of 201 — test expects 201 → RED
        return ResponseEntity.ok().build();
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        // STUB: always returns 200 with empty body — tests will fail → RED
        return ResponseEntity.ok().build();
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        // STUB: returns 200 — test for token field will fail → RED
        return ResponseEntity.ok().build();
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        // STUB: returns 200 with empty body — test for email field will fail → RED
        return ResponseEntity.ok().build();
    }
}
