package com.platform.idea;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.idea.domain.Idea;
import com.platform.idea.domain.IdeaStatus;
import com.platform.idea.dto.CreateIdeaRequest;
import com.platform.idea.repository.IdeaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class IdeaCreateTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean  IdeaRepository ideaRepository;

    @BeforeEach
    void setup() {
        when(ideaRepository.save(any())).thenAnswer(inv -> {
            Idea idea = inv.getArgument(0);
            idea.setId("idea-001");
            return idea;
        });
    }

    @Test
    void create_asFounder_returns201WithDraftStatus() throws Exception {
        CreateIdeaRequest req = new CreateIdeaRequest();
        req.setTitle("GreenTech Startup");
        req.setSummary("Solar energy for rural areas");

        mockMvc.perform(post("/ideas")
                .header("X-Internal-Request", "true")
                .header("X-User-Id",   "founder-1")
                .header("X-User-Role", "FOUNDER")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.founderId").value("founder-1"));
    }

    @Test
    void create_asInvestor_returns403() throws Exception {
        CreateIdeaRequest req = new CreateIdeaRequest();
        req.setTitle("GreenTech Startup");
        req.setSummary("Solar energy for rural areas");

        mockMvc.perform(post("/ideas")
                .header("X-Internal-Request", "true")
                .header("X-User-Id",   "investor-1")
                .header("X-User-Role", "INVESTOR")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    void create_blankTitle_returns400() throws Exception {
        CreateIdeaRequest req = new CreateIdeaRequest();
        req.setTitle("");
        req.setSummary("Some summary");

        mockMvc.perform(post("/ideas")
                .header("X-Internal-Request", "true")
                .header("X-User-Id",   "founder-1")
                .header("X-User-Role", "FOUNDER")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_missingInternalHeader_returns403() throws Exception {
        CreateIdeaRequest req = new CreateIdeaRequest();
        req.setTitle("GreenTech");
        req.setSummary("Summary");

        mockMvc.perform(post("/ideas")
                .header("X-User-Id",   "founder-1")
                .header("X-User-Role", "FOUNDER")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }
}
