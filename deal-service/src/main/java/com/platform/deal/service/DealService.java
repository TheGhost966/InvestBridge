package com.platform.deal.service;

import com.platform.deal.domain.*;
import com.platform.deal.dto.*;
import com.platform.deal.exception.ResourceNotFoundException;
import com.platform.deal.repository.*;
import org.springframework.stereotype.Service;

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
        throw new UnsupportedOperationException("TODO");
    }

    public InvestorProfile getProfile(String investorId) {
        throw new UnsupportedOperationException("TODO");
    }

    public Offer createOffer(String investorId, CreateOfferRequest req) {
        throw new UnsupportedOperationException("TODO");
    }

    public Offer getOffer(String offerId) {
        throw new UnsupportedOperationException("TODO");
    }

    public Offer acceptOffer(String offerId, String founderId) {
        throw new UnsupportedOperationException("TODO");
    }

    public Offer rejectOffer(String offerId, String founderId) {
        throw new UnsupportedOperationException("TODO");
    }

    public List<Match> getMatches(String userId, String role) {
        throw new UnsupportedOperationException("TODO");
    }

    public AbuseReport createReport(String reporterId, AbuseReportRequest req) {
        throw new UnsupportedOperationException("TODO");
    }
}
