package com.platform.deal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.deal.domain.AbuseReport;
import com.platform.deal.domain.Match;
import com.platform.deal.dto.AbuseReportRequest;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DealMatchReportTest {

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

    // --- GET /deals/matches ---

    @Test
    void getMatches_asInvestor_returns200WithList() throws Exception {
        Match m = new Match();
        m.setId("match1");
        m.setInvestorId("inv1");
        m.setFounderId("founder1");
        m.setIdeaId("idea1");
        m.setOfferId("off1");

        when(dealService.getMatches("inv1", "INVESTOR")).thenReturn(List.of(m));

        mockMvc.perform(get("/deals/matches")
                        .header(INTERNAL, "true")
                        .header(USER_ID, "inv1")
                        .header(USER_ROLE, "INVESTOR"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$[0].id").value("match1"))
               .andExpect(jsonPath("$[0].investorId").value("inv1"));
    }

    @Test
    void getMatches_asFounder_returns200WithList() throws Exception {
        Match m = new Match();
        m.setId("match2");
        m.setInvestorId("inv1");
        m.setFounderId("founder1");
        m.setIdeaId("idea1");
        m.setOfferId("off1");

        when(dealService.getMatches("founder1", "FOUNDER")).thenReturn(List.of(m));

        mockMvc.perform(get("/deals/matches")
                        .header(INTERNAL, "true")
                        .header(USER_ID, "founder1")
                        .header(USER_ROLE, "FOUNDER"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$[0].founderId").value("founder1"));
    }

    @Test
    void getMatches_missingInternalHeader_returns403() throws Exception {
        mockMvc.perform(get("/deals/matches")
                        .header(USER_ID, "inv1")
                        .header(USER_ROLE, "INVESTOR"))
               .andExpect(status().isForbidden());
    }

    @Test
    void getMatches_emptyList_returns200() throws Exception {
        when(dealService.getMatches("inv2", "INVESTOR")).thenReturn(List.of());

        mockMvc.perform(get("/deals/matches")
                        .header(INTERNAL, "true")
                        .header(USER_ID, "inv2")
                        .header(USER_ROLE, "INVESTOR"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$").isArray())
               .andExpect(jsonPath("$").isEmpty());
    }

    // --- POST /deals/reports ---

    @Test
    void createReport_validRequest_returns201() throws Exception {
        AbuseReport report = new AbuseReport();
        report.setId("rep1");
        report.setReporterId("inv1");
        report.setTargetId("founder1");
        report.setReason("Scam project");

        when(dealService.createReport(eq("inv1"), any(AbuseReportRequest.class))).thenReturn(report);

        AbuseReportRequest req = new AbuseReportRequest();
        req.setTargetId("founder1");
        req.setReason("Scam project");

        mockMvc.perform(post("/deals/reports")
                        .header(INTERNAL, "true")
                        .header(USER_ID, "inv1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
               .andExpect(status().isCreated())
               .andExpect(jsonPath("$.id").value("rep1"))
               .andExpect(jsonPath("$.reason").value("Scam project"));
    }

    @Test
    void createReport_missingReason_returns400() throws Exception {
        mockMvc.perform(post("/deals/reports")
                        .header(INTERNAL, "true")
                        .header(USER_ID, "inv1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetId\":\"founder1\"}"))
               .andExpect(status().isBadRequest());
    }

    @Test
    void createReport_missingInternalHeader_returns403() throws Exception {
        mockMvc.perform(post("/deals/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetId\":\"founder1\",\"reason\":\"Scam\"}"))
               .andExpect(status().isForbidden());
    }
}
