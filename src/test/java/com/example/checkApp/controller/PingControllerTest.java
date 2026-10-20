package com.example.checkApp.controller;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.checkApp.dto.response.PingResponse;
import com.example.checkApp.model.CheckStatus;
import com.example.checkApp.service.PingService;

@WebMvcTest(PingController.class)
class PingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PingService pingService;

    @Test
    void returnsOkWithPingResult() throws Exception {
        when(pingService.ping("https://example.com"))
                .thenReturn(new PingResponse("https://example.com", CheckStatus.UP, 42L, 200, null));

        mockMvc.perform(post("/api/ping")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"https://example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value("https://example.com"))
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.httpCode").value(200))
                .andExpect(jsonPath("$.responseTimeMs").value(42));
    }

    @Test
    void returnsBadRequestForInvalidUrl() throws Exception {
        mockMvc.perform(post("/api/ping")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"not-a-url\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors.url").exists());
    }

    @Test
    void returnsBadRequestForBlankUrl() throws Exception {
        mockMvc.perform(post("/api/ping")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.url").exists());
    }

    @Test
    void returnsBadRequestForMalformedJson() throws Exception {
        mockMvc.perform(post("/api/ping")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed request body"));
    }

    @Test
    void passesUrlToService() throws Exception {
        when(pingService.ping(anyString()))
                .thenReturn(new PingResponse("https://example.com", CheckStatus.DOWN, 5000L, null,
                        "Connect timed out"));

        mockMvc.perform(post("/api/ping")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"https://example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DOWN"))
                .andExpect(jsonPath("$.errorMessage").value("Connect timed out"));
    }
}
