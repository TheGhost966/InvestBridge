package com.platform.deal.controller;

import com.platform.deal.dto.*;
import com.platform.deal.service.DealService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/deals")
public class DealController {

    private final DealService dealService;

    public DealController(DealService dealService) {
        this.dealService = dealService;
    }

    // POST /deals/profiles → 201 Created  (stub: returns 200)
    @PostMapping("/profiles")
    public ResponseEntity<?> createProfile(@RequestHeader("X-User-Id") String userId,
                                           @RequestHeader("X-User-Role") String role,
                                           @Valid @RequestBody CreateProfileRequest req) {
        return ResponseEntity.ok().build();
    }

    // GET /deals/profiles/me → 200 OK  (stub: returns 204)
    @GetMapping("/profiles/me")
    public ResponseEntity<?> getProfile(@RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.noContent().build();
    }

    // POST /deals/offers → 201 Created  (stub: returns 200)
    @PostMapping("/offers")
    public ResponseEntity<?> createOffer(@RequestHeader("X-User-Id") String userId,
                                         @RequestHeader("X-User-Role") String role,
                                         @Valid @RequestBody CreateOfferRequest req) {
        return ResponseEntity.ok().build();
    }

    // GET /deals/offers/{id} → 200 OK  (stub: returns 204)
    @GetMapping("/offers/{id}")
    public ResponseEntity<?> getOffer(@PathVariable String id,
                                      @RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.noContent().build();
    }

    // PATCH /deals/offers/{id}/accept → 200 OK  (stub: returns 204)
    @PatchMapping("/offers/{id}/accept")
    public ResponseEntity<?> acceptOffer(@PathVariable String id,
                                         @RequestHeader("X-User-Id") String userId,
                                         @RequestHeader("X-User-Role") String role) {
        return ResponseEntity.noContent().build();
    }

    // PATCH /deals/offers/{id}/reject → 200 OK  (stub: returns 204)
    @PatchMapping("/offers/{id}/reject")
    public ResponseEntity<?> rejectOffer(@PathVariable String id,
                                         @RequestHeader("X-User-Id") String userId,
                                         @RequestHeader("X-User-Role") String role) {
        return ResponseEntity.noContent().build();
    }

    // GET /deals/matches → 200 OK  (stub: returns 204)
    @GetMapping("/matches")
    public ResponseEntity<?> getMatches(@RequestHeader("X-User-Id") String userId,
                                        @RequestHeader("X-User-Role") String role) {
        return ResponseEntity.noContent().build();
    }

    // POST /deals/reports → 201 Created  (stub: returns 200)
    @PostMapping("/reports")
    public ResponseEntity<?> createReport(@RequestHeader("X-User-Id") String userId,
                                          @Valid @RequestBody AbuseReportRequest req) {
        return ResponseEntity.ok().build();
    }
}
