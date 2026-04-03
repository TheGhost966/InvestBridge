package com.platform.idea.service;

import com.platform.idea.domain.Idea;
import com.platform.idea.domain.IdeaStatus;
import com.platform.idea.dto.CreateIdeaRequest;
import com.platform.idea.exception.IdeaNotFoundException;
import com.platform.idea.repository.IdeaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class IdeaService {

    private static final Logger log = LoggerFactory.getLogger(IdeaService.class);

    private final IdeaRepository ideaRepository;

    public IdeaService(IdeaRepository ideaRepository) {
        this.ideaRepository = ideaRepository;
    }

    public Idea create(CreateIdeaRequest req, String founderId) {
        Idea idea = new Idea();
        idea.setTitle(req.getTitle());
        idea.setSummary(req.getSummary());
        idea.setMarket(req.getMarket());
        idea.setTraction(req.getTraction());
        idea.setFundingNeeded(req.getFundingNeeded());
        idea.setLocation(req.getLocation());
        idea.setTags(req.getTags());
        idea.setFounderId(founderId);
        idea.setStatus(IdeaStatus.DRAFT);
        log.info("Creating idea founderId={} title={}", founderId, idea.getTitle());
        return ideaRepository.save(idea);
    }

    public Page<Idea> list(String role, String userId, Pageable pageable) {
        return switch (role.toUpperCase()) {
            case "INVESTOR" -> ideaRepository.findByStatus(IdeaStatus.VERIFIED, pageable);
            case "ADMIN"    -> ideaRepository.findAll(pageable);
            case "FOUNDER"  -> ideaRepository.findByFounderId(userId, pageable);
            default -> throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Unknown role");
        };
    }

    public Idea getById(String id) {
        return ideaRepository.findById(id)
                .orElseThrow(() -> new IdeaNotFoundException(id));
    }

    public Idea update(String id, CreateIdeaRequest req, String userId) {
        Idea idea = getById(id);
        requireOwner(idea, userId);
        requireStatus(idea, IdeaStatus.DRAFT, "Only DRAFT ideas can be updated");
        idea.setTitle(req.getTitle());
        idea.setSummary(req.getSummary());
        idea.setMarket(req.getMarket());
        idea.setTraction(req.getTraction());
        idea.setFundingNeeded(req.getFundingNeeded());
        idea.setLocation(req.getLocation());
        idea.setTags(req.getTags());
        return ideaRepository.save(idea);
    }

    public void delete(String id, String userId) {
        Idea idea = getById(id);
        requireOwner(idea, userId);
        requireStatus(idea, IdeaStatus.DRAFT, "Only DRAFT ideas can be deleted");
        ideaRepository.delete(idea);
        log.info("Deleted idea id={}", id);
    }

    public Idea verify(String id) {
        Idea idea = getById(id);
        idea.setStatus(IdeaStatus.VERIFIED);
        log.info("Verified idea id={}", id);
        return ideaRepository.save(idea);
    }

    public Idea reject(String id, String reason) {
        Idea idea = getById(id);
        idea.setStatus(IdeaStatus.REJECTED);
        idea.setRejectionReason(reason);
        log.info("Rejected idea id={} reason={}", id, reason);
        return ideaRepository.save(idea);
    }

    // ── helpers ────────────────────────────────────────────────────────────

    private void requireOwner(Idea idea, String userId) {
        if (!idea.getFounderId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not the owner of this idea");
        }
    }

    private void requireStatus(Idea idea, IdeaStatus required, String message) {
        if (idea.getStatus() != required) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, message);
        }
    }
}
