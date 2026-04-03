package com.platform.idea;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.idea.domain.Idea;
import com.platform.idea.domain.IdeaStatus;
import com.platform.idea.dto.CreateIdeaRequest;
import com.platform.idea.dto.RejectRequest;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class IdeaWorkflowTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean  IdeaRepository ideaRepository;

    private Idea draftIdea;
    private Idea submittedIdea;

    @BeforeEach
    void setup() {
        draftIdea = new Idea();
        draftIdea.setId("idea-draft");
        draftIdea.setTitle("My Startup");
        draftIdea.setSummary("Summary");
        draftIdea.setStatus(IdeaStatus.DRAFT);
        draftIdea.setFounderId("founder-1");

        submittedIdea = new Idea();
        submittedIdea.setId("idea-submitted");
        submittedIdea.setTitle("My Startup");
        submittedIdea.setSummary("Summary");
        submittedIdea.setStatus(IdeaStatus.SUBMITTED);
        submittedIdea.setFounderId("founder-1");

        when(ideaRepository.findById("idea-draft")).thenReturn(Optional.of(draftIdea));
        when(ideaRepository.findById("idea-submitted")).thenReturn(Optional.of(submittedIdea));
        when(ideaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    // ── UPDATE ─────────────────────────────────────────────────────────────

    @Test
    void update_founderOwnDraft_returns200() throws Exception {
        CreateIdeaRequest req = new CreateIdeaRequest();
        req.setTitle("Updated Title");
        req.setSummary("Updated summary");

        mockMvc.perform(put("/ideas/idea-draft")
                .header("X-Internal-Request", "true")
                .header("X-User-Id",   "founder-1")
                .header("X-User-Role", "FOUNDER")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    void update_founderNotOwner_returns403() throws Exception {
        CreateIdeaRequest req = new CreateIdeaRequest();
        req.setTitle("Hack");
        req.setSummary("Hacking");

        mockMvc.perform(put("/ideas/idea-draft")
                .header("X-Internal-Request", "true")
                .header("X-User-Id",   "other-founder")
                .header("X-User-Role", "FOUNDER")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    void update_nonDraft_returns403() throws Exception {
        CreateIdeaRequest req = new CreateIdeaRequest();
        req.setTitle("Updated");
        req.setSummary("Summary");

        mockMvc.perform(put("/ideas/idea-submitted")
                .header("X-Internal-Request", "true")
                .header("X-User-Id",   "founder-1")
                .header("X-User-Role", "FOUNDER")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    // ── DELETE ─────────────────────────────────────────────────────────────

    @Test
    void delete_founderOwnDraft_returns204() throws Exception {
        mockMvc.perform(delete("/ideas/idea-draft")
                .header("X-Internal-Request", "true")
                .header("X-User-Id",   "founder-1")
                .header("X-User-Role", "FOUNDER"))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_founderNotOwner_returns403() throws Exception {
        mockMvc.perform(delete("/ideas/idea-draft")
                .header("X-Internal-Request", "true")
                .header("X-User-Id",   "other-founder")
                .header("X-User-Role", "FOUNDER"))
                .andExpect(status().isForbidden());
    }

    // ── VERIFY / REJECT ────────────────────────────────────────────────────

    @Test
    void verify_asAdmin_returns200() throws Exception {
        mockMvc.perform(patch("/ideas/idea-submitted/verify")
                .header("X-Internal-Request", "true")
                .header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("VERIFIED"));
    }

    @Test
    void verify_asInvestor_returns403() throws Exception {
        mockMvc.perform(patch("/ideas/idea-submitted/verify")
                .header("X-Internal-Request", "true")
                .header("X-User-Role", "INVESTOR"))
                .andExpect(status().isForbidden());
    }

    @Test
    void reject_asAdmin_returns200() throws Exception {
        RejectRequest req = new RejectRequest("Not viable");

        mockMvc.perform(patch("/ideas/idea-submitted/reject")
                .header("X-Internal-Request", "true")
                .header("X-User-Role", "ADMIN")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }
}
