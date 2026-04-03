package com.platform.deal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.deal.domain.Offer;
import com.platform.deal.domain.OfferStatus;
import com.platform.deal.dto.CreateOfferRequest;
import com.platform.deal.exception.ResourceNotFoundException;
import com.platform.deal.repository.AbuseReportRepository;
import com.platform.deal.repository.InvestorProfileRepository;
import com.platform.deal.repository.MatchRepository;
import com.platform.deal.repository.OfferRepository;
import com.platform.deal.service.DealService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DealOfferTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean DealService dealService;
    @MockBean InvestorProfileRepository investorProfileRepository;
    @MockBean OfferRepository offerRepository;
    @MockBean MatchRepository matchRepository;
    @MockBean AbuseReportRepository abuseReportRepository;

    private static final String INTERNAL  = "X-Internal-Request";
    private static final String USER_ID   = "X-User-Id";
    private static final String USER_ROLE = "X-User-Role";

    private Offer buildOffer(String id, String investorId, String founderId, OfferStatus status) {
        Offer o = new Offer();
        o.setId(id);
        o.setInvestorId(investorId);
        o.setFounderId(founderId);
        o.setIdeaId("idea1");
        o.setAmount(5000.0);
        o.setStatus(status);
        return o;
    }

    // --- POST /deals/offers ---

    @Test
    void createOffer_validInvestor_returns201() throws Exception {
        Offer saved = buildOffer("off1", "inv1", "founder1", OfferStatus.PENDING);
        when(dealService.createOffer(eq("inv1"), any(CreateOfferRequest.class))).thenReturn(saved);

        CreateOfferRequest req = new CreateOfferRequest();
        req.setIdeaId("idea1");
        req.setFounderId("founder1");
        req.setAmount(5000.0);
        req.setMessage("Interested in your idea");

        mockMvc.perform(post("/deals/offers")
                        .header(INTERNAL, "true")
                        .header(USER_ID, "inv1")
                        .header(USER_ROLE, "INVESTOR")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
               .andExpect(status().isCreated())
               .andExpect(jsonPath("$.id").value("off1"))
               .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void createOffer_missingIdeaId_returns400() throws Exception {
        mockMvc.perform(post("/deals/offers")
                        .header(INTERNAL, "true")
                        .header(USER_ID, "inv1")
                        .header(USER_ROLE, "INVESTOR")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"founderId\":\"founder1\",\"amount\":5000}"))
               .andExpect(status().isBadRequest());
    }

    @Test
    void createOffer_missingInternalHeader_returns403() throws Exception {
        mockMvc.perform(post("/deals/offers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ideaId\":\"idea1\",\"founderId\":\"f1\",\"amount\":5000}"))
               .andExpect(status().isForbidden());
    }

    // --- GET /deals/offers/{id} ---

    @Test
    void getOffer_existingId_returns200() throws Exception {
        Offer offer = buildOffer("off1", "inv1", "founder1", OfferStatus.PENDING);
        when(dealService.getOffer("off1")).thenReturn(offer);

        mockMvc.perform(get("/deals/offers/off1")
                        .header(INTERNAL, "true")
                        .header(USER_ID, "inv1"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.id").value("off1"));
    }

    @Test
    void getOffer_notFound_returns404() throws Exception {
        when(dealService.getOffer("missing")).thenThrow(new ResourceNotFoundException("Offer", "missing"));

        mockMvc.perform(get("/deals/offers/missing")
                        .header(INTERNAL, "true")
                        .header(USER_ID, "inv1"))
               .andExpect(status().isNotFound());
    }

    // --- PATCH /deals/offers/{id}/accept ---

    @Test
    void acceptOffer_byFounder_returns200() throws Exception {
        Offer accepted = buildOffer("off1", "inv1", "founder1", OfferStatus.ACCEPTED);
        when(dealService.acceptOffer("off1", "founder1")).thenReturn(accepted);

        mockMvc.perform(patch("/deals/offers/off1/accept")
                        .header(INTERNAL, "true")
                        .header(USER_ID, "founder1")
                        .header(USER_ROLE, "FOUNDER"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.status").value("ACCEPTED"));
    }

    @Test
    void acceptOffer_notFound_returns404() throws Exception {
        when(dealService.acceptOffer("bad", "founder1"))
                .thenThrow(new ResourceNotFoundException("Offer", "bad"));

        mockMvc.perform(patch("/deals/offers/bad/accept")
                        .header(INTERNAL, "true")
                        .header(USER_ID, "founder1")
                        .header(USER_ROLE, "FOUNDER"))
               .andExpect(status().isNotFound());
    }

    // --- PATCH /deals/offers/{id}/reject ---

    @Test
    void rejectOffer_byFounder_returns200() throws Exception {
        Offer rejected = buildOffer("off1", "inv1", "founder1", OfferStatus.REJECTED);
        when(dealService.rejectOffer("off1", "founder1")).thenReturn(rejected);

        mockMvc.perform(patch("/deals/offers/off1/reject")
                        .header(INTERNAL, "true")
                        .header(USER_ID, "founder1")
                        .header(USER_ROLE, "FOUNDER"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.status").value("REJECTED"));
    }
}
