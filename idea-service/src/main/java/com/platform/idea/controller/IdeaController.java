package com.platform.idea.controller;

import com.platform.idea.dto.CreateIdeaRequest;
import com.platform.idea.dto.RejectRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** STUB — wrong responses intentionally. Tests FAIL (RED). */
@RestController
@RequestMapping("/ideas")
public class IdeaController {

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody CreateIdeaRequest req,
                                    @RequestHeader(value = "X-User-Id",   required = false) String userId,
                                    @RequestHeader(value = "X-User-Role", required = false) String role) {
        return ResponseEntity.ok().build(); // stub: 200 instead of 201
    }

    @GetMapping
    public ResponseEntity<?> list(@RequestHeader(value = "X-User-Id",   required = false) String userId,
                                  @RequestHeader(value = "X-User-Role", required = false) String role,
                                  @RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok().build(); // stub: empty body
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable String id,
                                     @RequestHeader(value = "X-User-Role", required = false) String role) {
        return ResponseEntity.ok().build(); // stub
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable String id,
                                    @Valid @RequestBody CreateIdeaRequest req,
                                    @RequestHeader(value = "X-User-Id",   required = false) String userId,
                                    @RequestHeader(value = "X-User-Role", required = false) String role) {
        return ResponseEntity.ok().build(); // stub
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable String id,
                                    @RequestHeader(value = "X-User-Id",   required = false) String userId,
                                    @RequestHeader(value = "X-User-Role", required = false) String role) {
        return ResponseEntity.ok().build(); // stub: 200 instead of 204
    }

    @PatchMapping("/{id}/verify")
    public ResponseEntity<?> verify(@PathVariable String id,
                                    @RequestHeader(value = "X-User-Role", required = false) String role) {
        return ResponseEntity.ok().build(); // stub
    }

    @PatchMapping("/{id}/reject")
    public ResponseEntity<?> reject(@PathVariable String id,
                                    @Valid @RequestBody RejectRequest req,
                                    @RequestHeader(value = "X-User-Role", required = false) String role) {
        return ResponseEntity.ok().build(); // stub
    }
}
