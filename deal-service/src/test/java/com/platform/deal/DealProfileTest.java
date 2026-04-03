package com.platform.deal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.deal.domain.InvestorProfile;
import com.platform.deal.dto.CreateProfileRequest;
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

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DealProfileTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean DealService dealService;
    @MockBean InvestorProfileRepository investorProfileRepository;
    @MockBean OfferRepository offerRepository;
    @MockBean MatchRepository matchRepository;
    @MockBean AbuseReportRepository abuseReportRepository;

    private static final String INTERNAL = "X-Internal-Request";
    private static final String USER_ID  = "X-User-Id";
    private static final String USER_ROLE = "X-User-Role";

    // --- POST /deals/profiles ---

    @Test
    void createProfile_missingInternalHeader_returns403() throws Exception {
        mockMvc.perform(post("/deals/profiles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bio\":\"tech investor\",\"minInvestment\":1000,\"maxInvestment\":50000}"))
               .andExpect(status().isForbidden());
    }

    @Test
    void createProfile_validInvestor_returns201() throws Exception {
        InvestorProfile profile = new InvestorProfile();
        profile.setId("p1");
        profile.setInvestorId("inv1");
        profile.setBio("tech investor");
        profile.setSectors(List.of("tech"));
        profile.setMinInvestment(1000.0);
        profile.setMaxInvestment(50000.0);

        when(dealService.createProfile(eq("inv1"), any(CreateProfileRequest.class)))
                .thenReturn(profile);

        CreateProfileRequest req = new CreateProfileRequest();
        req.setBio("tech investor");
        req.setSectors(List.of("tech"));
        req.setMinInvestment(1000.0);
        req.setMaxInvestment(50000.0);

        mockMvc.perform(post("/deals/profiles")
                        .header(INTERNAL, "true")
                        .header(USER_ID, "inv1")
                        .header(USER_ROLE, "INVESTOR")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
               .andExpect(status().isCreated())
               .andExpect(jsonPath("$.investorId").value("inv1"));
    }

    @Test
    void createProfile_missingBio_returns400() throws Exception {
        mockMvc.perform(post("/deals/profiles")
                        .header(INTERNAL, "true")
                        .header(USER_ID, "inv1")
                        .header(USER_ROLE, "INVESTOR")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"minInvestment\":1000,\"maxInvestment\":50000}"))
               .andExpect(status().isBadRequest());
    }

    // --- GET /deals/profiles/me ---

    @Test
    void getProfile_validRequest_returns200() throws Exception {
        InvestorProfile profile = new InvestorProfile();
        profile.setId("p1");
        profile.setInvestorId("inv1");
        profile.setBio("tech investor");

        when(dealService.getProfile("inv1")).thenReturn(profile);

        mockMvc.perform(get("/deals/profiles/me")
                        .header(INTERNAL, "true")
                        .header(USER_ID, "inv1"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.investorId").value("inv1"));
    }

    @Test
    void getProfile_notFound_returns404() throws Exception {
        when(dealService.getProfile("inv_missing"))
                .thenThrow(new com.platform.deal.exception.ResourceNotFoundException("InvestorProfile", "inv_missing"));

        mockMvc.perform(get("/deals/profiles/me")
                        .header(INTERNAL, "true")
                        .header(USER_ID, "inv_missing"))
               .andExpect(status().isNotFound());
    }
}
