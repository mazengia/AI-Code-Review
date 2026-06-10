package com.maze.mazecodereviewerai;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.maze.mazecodereviewerai.dto.CodeReviewRequest;
import com.maze.mazecodereviewerai.service.ClaudeLlmService;
import com.maze.mazecodereviewerai.service.NlpPipelineService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
        import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MazeCodeReviewerAiApplicationTests {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;

    @MockBean private ClaudeLlmService claudeLlmService;
    @MockBean private NlpPipelineService nlpPipelineService;

    @Test
    void healthEndpointShouldReturn200() throws Exception {
        mvc.perform(get("/code-review/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void reviewEndpointShouldRejectBlankCode() throws Exception {
        CodeReviewRequest req = CodeReviewRequest.builder()
                .code("")
                .language("java")
                .build();

        mvc.perform(post("/code-review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.code").exists());
    }

    @Test
    void statsEndpointShouldReturn200() throws Exception {
        mvc.perform(get("/code-review/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalReviews").exists());
    }
}