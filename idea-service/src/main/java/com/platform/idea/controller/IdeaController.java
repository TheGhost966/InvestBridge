package com.platform.idea.controller;

import com.platform.idea.domain.Idea;
import com.platform.idea.dto.CreateIdeaRequest;
import com.platform.idea.dto.RejectRequest;
import com.platform.idea.service.IdeaService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/ideas")
public class IdeaController {

    private final IdeaService ideaService;

    public IdeaController(IdeaService ideaService) {
        this.ideaService = ideaService;
    }

    /** POST /ideas → 201 (FOUNDER only) */
    @PostMapping
    public ResponseEntity<Idea> create(
            @Valid @RequestBody CreateIdeaRequest req,
            @RequestHeader(value = "X-User-Id",   required = false) String userId,
            @RequestHeader(value = "X-User-Role", required = false) String role) {

        requireRole(role, "FOUNDER");
        Idea idea = ideaService.create(req, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(idea);
    }

    /** GET /ideas → INVESTOR: verified only | ADMIN: all | FOUNDER: own */
    @GetMapping
    public ResponseEntity<Page<Idea>> list(
            @RequestHeader(value = "X-User-Id",   required = false) String userId,
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<Idea> result = ideaService.list(role, userId, PageRequest.of(page, size));
        return ResponseEntity.ok(result);
    }

    /** GET /ideas/{id} → 200 or 404 */
    @GetMapping("/{id}")
    public ResponseEntity<Idea> getById(
            @PathVariable String id,
            @RequestHeader(value = "X-User-Role", required = false) String role) {

        return ResponseEntity.ok(ideaService.getById(id));
    }

    /** PUT /ideas/{id} → 200 (FOUNDER: own DRAFT only) */
    @PutMapping("/{id}")
    public ResponseEntity<Idea> update(
            @PathVariable String id,
            @Valid @RequestBody CreateIdeaRequest req,
            @RequestHeader(value = "X-User-Id",   required = false) String userId,
            @RequestHeader(value = "X-User-Role", required = false) String role) {

        requireRole(role, "FOUNDER");
        return ResponseEntity.ok(ideaService.update(id, req, userId));
    }

    /** DELETE /ideas/{id} → 204 (FOUNDER: own DRAFT only) */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable String id,
            @RequestHeader(value = "X-User-Id",   required = false) String userId,
            @RequestHeader(value = "X-User-Role", required = false) String role) {

        requireRole(role, "FOUNDER");
        ideaService.delete(id, userId);
        return ResponseEntity.noContent().build();
    }

    /** PATCH /ideas/{id}/verify → 200 (ADMIN only) */
    @PatchMapping("/{id}/verify")
    public ResponseEntity<Idea> verify(
            @PathVariable String id,
            @RequestHeader(value = "X-User-Role", required = false) String role) {

        requireRole(role, "ADMIN");
        return ResponseEntity.ok(ideaService.verify(id));
    }

    /** PATCH /ideas/{id}/reject → 200 (ADMIN only) */
    @PatchMapping("/{id}/reject")
    public ResponseEntity<Idea> reject(
            @PathVariable String id,
            @Valid @RequestBody RejectRequest req,
            @RequestHeader(value = "X-User-Role", required = false) String role) {

        requireRole(role, "ADMIN");
        return ResponseEntity.ok(ideaService.reject(id, req.getReason()));
    }

    // ── helper ─────────────────────────────────────────────────────────────

    private void requireRole(String actual, String expected) {
        if (!expected.equalsIgnoreCase(actual)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Role " + expected + " required");
        }
    }
}
