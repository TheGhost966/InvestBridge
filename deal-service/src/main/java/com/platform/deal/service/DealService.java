package com.platform.deal.service;

import com.platform.deal.domain.*;
import com.platform.deal.dto.*;
import com.platform.deal.exception.ResourceNotFoundException;
import com.platform.deal.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class DealService {

    private final InvestorProfileRepository profileRepo;
    private final OfferRepository offerRepo;
    private final MatchRepository matchRepo;
    private final AbuseReportRepository reportRepo;

    public DealService(InvestorProfileRepository profileRepo,
                       OfferRepository offerRepo,
                       MatchRepository matchRepo,
                       AbuseReportRepository reportRepo) {
        this.profileRepo = profileRepo;
        this.offerRepo   = offerRepo;
        this.matchRepo   = matchRepo;
        this.reportRepo  = reportRepo;
    }

    public InvestorProfile createProfile(String investorId, CreateProfileRequest req) {
        profileRepo.findByInvestorId(investorId).ifPresent(p -> {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Profile already exists");
        });
        InvestorProfile profile = new InvestorProfile();
        profile.setInvestorId(investorId);
        profile.setBio(req.getBio());
        profile.setSectors(req.getSectors());
        profile.setMinInvestment(req.getMinInvestment());
        profile.setMaxInvestment(req.getMaxInvestment());
        return profileRepo.save(profile);
    }

    public InvestorProfile getProfile(String investorId) {
        return profileRepo.findByInvestorId(investorId)
                .orElseThrow(() -> new ResourceNotFoundException("InvestorProfile", investorId));
    }

    public Offer createOffer(String investorId, CreateOfferRequest req) {
        Offer offer = new Offer();
        offer.setInvestorId(investorId);
        offer.setIdeaId(req.getIdeaId());
        offer.setFounderId(req.getFounderId());
        offer.setAmount(req.getAmount());
        offer.setMessage(req.getMessage());
        offer.setStatus(OfferStatus.PENDING);
        return offerRepo.save(offer);
    }

    public Offer getOffer(String offerId) {
        return offerRepo.findById(offerId)
                .orElseThrow(() -> new ResourceNotFoundException("Offer", offerId));
    }

    public Offer acceptOffer(String offerId, String founderId) {
        Offer offer = offerRepo.findById(offerId)
                .orElseThrow(() -> new ResourceNotFoundException("Offer", offerId));
        if (!offer.getFounderId().equals(founderId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your offer");
        }
        offer.setStatus(OfferStatus.ACCEPTED);
        offerRepo.save(offer);

        Match match = new Match(offer.getInvestorId(), offer.getFounderId(),
                                offer.getIdeaId(), offer.getId());
        matchRepo.save(match);
        return offer;
    }

    public Offer rejectOffer(String offerId, String founderId) {
        Offer offer = offerRepo.findById(offerId)
                .orElseThrow(() -> new ResourceNotFoundException("Offer", offerId));
        if (!offer.getFounderId().equals(founderId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your offer");
        }
        offer.setStatus(OfferStatus.REJECTED);
        return offerRepo.save(offer);
    }

    public List<Match> getMatches(String userId, String role) {
        if ("INVESTOR".equalsIgnoreCase(role)) {
            return matchRepo.findByInvestorId(userId);
        }
        return matchRepo.findByFounderId(userId);
    }

    public AbuseReport createReport(String reporterId, AbuseReportRequest req) {
        AbuseReport report = new AbuseReport(reporterId, req.getTargetId(), req.getReason());
        return reportRepo.save(report);
    }
}
