package com.example.checkApp.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.checkApp.dto.request.CheckRequest;
import com.example.checkApp.dto.response.CheckResponse;
import com.example.checkApp.exception.NotFoundException;
import com.example.checkApp.model.CheckStatus;
import com.example.checkApp.service.CheckService;

@WebMvcTest(CheckController.class)
class CheckControllerTest {

    private static final String VALID_BODY = """
            {"name":"google","url":"https://google.com","checkIntervalSeconds":60,"enabled":true}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CheckService checkService;

    private CheckResponse sampleResponse() {
        return new CheckResponse(1L, "google", "https://google.com", 60, true,
                CheckStatus.UP, Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2025-12-31T00:00:00Z"));
    }

    @Test
    void createReturns201WithLocation() throws Exception {
        when(checkService.create(any(CheckRequest.class))).thenReturn(sampleResponse());

        mockMvc.perform(post("/api/checks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/checks/1"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("google"))
                .andExpect(jsonPath("$.status").doesNotExist());
    }

    @Test
    void createReturns400ForInvalidUrl() throws Exception {
        mockMvc.perform(post("/api/checks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"x\",\"url\":\"nope\",\"checkIntervalSeconds\":60,\"enabled\":true}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.url").exists());
    }

    @Test
    void createReturns400ForTooSmallInterval() throws Exception {
        mockMvc.perform(post("/api/checks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"x\",\"url\":\"https://x.com\",\"checkIntervalSeconds\":5,\"enabled\":true}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.checkIntervalSeconds").exists());
    }

    @Test
    void findAllReturnsList() throws Exception {
        when(checkService.findAll()).thenReturn(List.of(sampleResponse()));

        mockMvc.perform(get("/api/checks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("google"))
                .andExpect(jsonPath("$[0].lastStatus").value("UP"));
    }

    @Test
    void findByIdReturnsCheck() throws Exception {
        when(checkService.findById(1L)).thenReturn(sampleResponse());

        mockMvc.perform(get("/api/checks/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void findByIdReturns404WhenMissing() throws Exception {
        when(checkService.findById(99L)).thenThrow(new NotFoundException("Check", 99L));

        mockMvc.perform(get("/api/checks/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Check with id 99 not found"));
    }

    @Test
    void updateReturnsUpdatedCheck() throws Exception {
        when(checkService.update(eq(1L), any(CheckRequest.class))).thenReturn(sampleResponse());

        mockMvc.perform(put("/api/checks/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void deleteReturns204() throws Exception {
        mockMvc.perform(delete("/api/checks/1"))
                .andExpect(status().isNoContent());
        verify(checkService).delete(1L);
    }

    @Test
    void deleteReturns404WhenMissing() throws Exception {
        doThrow(new NotFoundException("Check", 42L)).when(checkService).delete(42L);

        mockMvc.perform(delete("/api/checks/42"))
                .andExpect(status().isNotFound());
    }
}
