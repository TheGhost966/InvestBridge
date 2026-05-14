package com.platform.desktop.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.platform.desktop.api.dto.AbuseReport;
import com.platform.desktop.api.dto.AbuseReportRequest;
import com.platform.desktop.api.dto.CreateOfferRequest;
import com.platform.desktop.api.dto.CreateProfileRequest;
import com.platform.desktop.api.dto.InvestorProfile;
import com.platform.desktop.api.dto.Match;
import com.platform.desktop.api.dto.Offer;

import java.util.List;

/**
 * Deal endpoints ({@code /deals/**}). Profiles are INVESTOR-only (the backend
 * rejects others). Offer accept/reject is FOUNDER-only. Listing offers and
 * matches is role-aware on the server side.
 */
public class DealApi {

    private final ApiClient client;

    public DealApi(ApiClient client) {
        this.client = client;
    }

    // ── investor profiles ─────────────────────────────────────────────────────

    public InvestorProfile createProfile(CreateProfileRequest req) {
        return client.post("/deals/profiles", req, InvestorProfile.class);
    }

    /** @return the current investor's profile, or {@code null} if none exists yet (404). */
    public InvestorProfile getMyProfile() {
        try {
            return client.get("/deals/profiles/me", InvestorProfile.class);
        } catch (ApiException e) {
            if (e.isNotFound()) return null;
            throw e;
        }
    }

    // ── offers ────────────────────────────────────────────────────────────────

    public Offer makeOffer(CreateOfferRequest req) {
        return client.post("/deals/offers", req, Offer.class);
    }

    public Offer getOffer(String offerId) {
        return client.get("/deals/offers/" + offerId, Offer.class);
    }

    /**
     * Role-aware list:
     * <ul>
     *   <li>INVESTOR — own offers (outgoing)</li>
     *   <li>FOUNDER  — offers received on own ideas (incoming)</li>
     *   <li>ADMIN    — every offer in the system</li>
     * </ul>
     */
    public List<Offer> listOffers() {
        return client.get("/deals/offers", new TypeReference<List<Offer>>() {});
    }

    public Offer acceptOffer(String offerId) {
        return client.patch("/deals/offers/" + offerId + "/accept", null, Offer.class);
    }

    public Offer rejectOffer(String offerId) {
        return client.patch("/deals/offers/" + offerId + "/reject", null, Offer.class);
    }

    // ── matches ───────────────────────────────────────────────────────────────

    public List<Match> getMatches() {
        return client.get("/deals/matches", new TypeReference<List<Match>>() {});
    }

    // ── reports ───────────────────────────────────────────────────────────────

    public AbuseReport createReport(AbuseReportRequest req) {
        return client.post("/deals/reports", req, AbuseReport.class);
    }
}
