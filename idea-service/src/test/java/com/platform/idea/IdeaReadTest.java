package com.platform.idea;

import com.platform.idea.domain.Idea;
import com.platform.idea.domain.IdeaStatus;
import com.platform.idea.repository.IdeaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class IdeaReadTest {

    @Autowired MockMvc mockMvc;
    @MockBean  IdeaRepository ideaRepository;

    private Idea verifiedIdea;
    private Idea draftIdea;

    @BeforeEach
    void setup() {
        verifiedIdea = new Idea();
        verifiedIdea.setId("idea-001");
        verifiedIdea.setTitle("Verified Idea");
        verifiedIdea.setSummary("Summary");
        verifiedIdea.setStatus(IdeaStatus.VERIFIED);
        verifiedIdea.setFounderId("founder-1");

        draftIdea = new Idea();
        draftIdea.setId("idea-002");
        draftIdea.setTitle("Draft Idea");
        draftIdea.setSummary("Summary");
        draftIdea.setStatus(IdeaStatus.DRAFT);
        draftIdea.setFounderId("founder-1");

        when(ideaRepository.findByStatus(eq(IdeaStatus.VERIFIED), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(verifiedIdea)));
        when(ideaRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(verifiedIdea, draftIdea)));
        when(ideaRepository.findByFounderId(eq("founder-1"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(verifiedIdea, draftIdea)));
        when(ideaRepository.findById("idea-001")).thenReturn(Optional.of(verifiedIdea));
        when(ideaRepository.findById("bad-id")).thenReturn(Optional.empty());
    }

    @Test
    void list_asInvestor_returnsOnlyVerified() throws Exception {
        mockMvc.perform(get("/ideas")
                .header("X-Internal-Request", "true")
                .header("X-User-Id",   "investor-1")
                .header("X-User-Role", "INVESTOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("VERIFIED"))
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    void list_asAdmin_returnsAll() throws Exception {
        mockMvc.perform(get("/ideas")
                .header("X-Internal-Request", "true")
                .header("X-User-Id",   "admin-1")
                .header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2));
    }

    @Test
    void list_asFounder_returnsOwnIdeas() throws Exception {
        mockMvc.perform(get("/ideas")
                .header("X-Internal-Request", "true")
                .header("X-User-Id",   "founder-1")
                .header("X-User-Role", "FOUNDER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2));
    }

    @Test
    void getById_exists_returns200() throws Exception {
        mockMvc.perform(get("/ideas/idea-001")
                .header("X-Internal-Request", "true")
                .header("X-User-Role", "INVESTOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("idea-001"));
    }

    @Test
    void getById_notFound_returns404() throws Exception {
        mockMvc.perform(get("/ideas/bad-id")
                .header("X-Internal-Request", "true")
                .header("X-User-Role", "INVESTOR"))
                .andExpect(status().isNotFound());
    }

    @Test
    void list_missingInternalHeader_returns403() throws Exception {
        mockMvc.perform(get("/ideas")
                .header("X-User-Role", "INVESTOR"))
                .andExpect(status().isForbidden());
    }
}
