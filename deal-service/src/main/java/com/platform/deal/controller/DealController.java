package com.platform.deal.controller;

import com.platform.deal.dto.*;
import com.platform.deal.service.DealService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/deals")
public class DealController {

    private final DealService dealService;

    public DealController(DealService dealService) {
        this.dealService = dealService;
    }

    @PostMapping("/profiles")
    public ResponseEntity<?> createProfile(@RequestHeader("X-User-Id") String userId,
                                           @RequestHeader("X-User-Role") String role,
                                           @Valid @RequestBody CreateProfileRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(dealService.createProfile(userId, req));
    }

    @GetMapping("/profiles/me")
    public ResponseEntity<?> getProfile(@RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(dealService.getProfile(userId));
    }

    @PostMapping("/offers")
    public ResponseEntity<?> createOffer(@RequestHeader("X-User-Id") String userId,
                                         @RequestHeader("X-User-Role") String role,
                                         @Valid @RequestBody CreateOfferRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(dealService.createOffer(userId, req));
    }

    @GetMapping("/offers/{id}")
    public ResponseEntity<?> getOffer(@PathVariable String id,
                                      @RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(dealService.getOffer(id));
    }

    @PatchMapping("/offers/{id}/accept")
    public ResponseEntity<?> acceptOffer(@PathVariable String id,
                                         @RequestHeader("X-User-Id") String userId,
                                         @RequestHeader("X-User-Role") String role) {
        return ResponseEntity.ok(dealService.acceptOffer(id, userId));
    }

    @PatchMapping("/offers/{id}/reject")
    public ResponseEntity<?> rejectOffer(@PathVariable String id,
                                         @RequestHeader("X-User-Id") String userId,
                                         @RequestHeader("X-User-Role") String role) {
        return ResponseEntity.ok(dealService.rejectOffer(id, userId));
    }

    @GetMapping("/matches")
    public ResponseEntity<?> getMatches(@RequestHeader("X-User-Id") String userId,
                                        @RequestHeader("X-User-Role") String role) {
        return ResponseEntity.ok(dealService.getMatches(userId, role));
    }

    @PostMapping("/reports")
    public ResponseEntity<?> createReport(@RequestHeader("X-User-Id") String userId,
                                          @Valid @RequestBody AbuseReportRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(dealService.createReport(userId, req));
    }
}
