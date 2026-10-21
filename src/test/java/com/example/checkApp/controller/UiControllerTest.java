package com.example.checkApp.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.checkApp.config.SecurityConfig;
import com.example.checkApp.dto.request.CheckRequest;
import com.example.checkApp.dto.request.RegisterRequest;
import com.example.checkApp.dto.response.CheckResponse;
import com.example.checkApp.dto.response.CheckResultResponse;
import com.example.checkApp.dto.response.CheckStatsResponse;
import com.example.checkApp.dto.response.PageResponse;
import com.example.checkApp.exception.ConflictException;
import com.example.checkApp.model.CheckStatus;
import com.example.checkApp.service.CheckHistoryService;
import com.example.checkApp.service.CheckService;
import com.example.checkApp.service.UserService;

@WebMvcTest(UiController.class)
@Import({SecurityConfig.class, GlobalModelAdvice.class})
class UiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CheckService checkService;

    @MockitoBean
    private CheckHistoryService checkHistoryService;

    @MockitoBean
    private UserService userService;

    private CheckResponse sampleCheck() {
        return new CheckResponse(1L, "google", "https://google.com", 60, true,
                CheckStatus.UP, Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2025-12-31T00:00:00Z"));
    }

    @Test
    void dashboardRedirectsAnonymousToLogin() throws Exception {
        mockMvc.perform(get("/").accept(MediaType.TEXT_HTML))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void dashboardShowsChecksForUser() throws Exception {
        when(checkService.findAll()).thenReturn(List.of(sampleCheck()));
        when(checkHistoryService.getStats(eq(1L), anyInt()))
                .thenReturn(new CheckStatsResponse(1L, 24, 10, 9, 1, 90.0, 42L, 100L));

        mockMvc.perform(get("/").with(user("alice")))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attributeExists("checks", "checkRequest", "stats"));
    }

    @Test
    void loginPageIsPublic() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"));
    }

    @Test
    void registerPageIsPublic() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"));
    }

    @Test
    void createCheckRedirectsOnSuccess() throws Exception {
        mockMvc.perform(post("/checks")
                        .with(user("alice"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("name", "google")
                        .param("url", "https://google.com")
                        .param("checkIntervalSeconds", "60")
                        .param("enabled", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));

        verify(checkService).create(any(CheckRequest.class));
    }

    @Test
    void createCheckRendersFormWithErrorsOnInvalidInput() throws Exception {
        mockMvc.perform(post("/checks")
                        .with(user("alice"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("name", "")
                        .param("url", "nope")
                        .param("checkIntervalSeconds", "5")
                        .param("enabled", "true"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attributeHasFieldErrors("checkRequest", "name", "url", "checkIntervalSeconds"));

        verify(checkService, never()).create(any());
    }

    @Test
    void toggleFlipsEnabledFlag() throws Exception {
        when(checkService.findById(1L)).thenReturn(sampleCheck());

        mockMvc.perform(post("/checks/1/toggle")
                        .with(user("alice"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));

        verify(checkService).update(eq(1L), any(CheckRequest.class));
    }

    @Test
    void deleteRemovesCheck() throws Exception {
        mockMvc.perform(post("/checks/1/delete")
                        .with(user("alice"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));

        verify(checkService).delete(1L);
    }

    @Test
    void detailShowsStatsAndHistory() throws Exception {
        when(checkService.findById(1L)).thenReturn(sampleCheck());
        when(checkHistoryService.getStats(1L, 24))
                .thenReturn(new CheckStatsResponse(1L, 24, 10, 9, 1, 90.0, 42L, 100L));
        when(checkHistoryService.getResults(1L, 0, 20))
                .thenReturn(new PageResponse<>(List.of(
                        new CheckResultResponse(10L, CheckStatus.UP, 200, 42L, null,
                                Instant.parse("2026-01-01T00:00:00Z"))), 0, 20, 1, 1));

        mockMvc.perform(get("/checks/1").with(user("alice")))
                .andExpect(status().isOk())
                .andExpect(view().name("check-detail"));
    }

    @Test
    void detailRedirectsWhenCheckMissing() throws Exception {
        when(checkService.findById(404L))
                .thenThrow(new com.example.checkApp.exception.NotFoundException("Check", 404L));

        mockMvc.perform(get("/checks/404").with(user("alice")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }

    @Test
    void registerFormCreatesUserAndRedirectsToLogin() throws Exception {
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("username", "alice")
                        .param("password", "password123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?registered"));

        verify(userService).register(any(RegisterRequest.class));
    }

    @Test
    void registerFormShowsConflictError() throws Exception {
        org.mockito.Mockito.doThrow(new ConflictException("User", "alice"))
                .when(userService).register(any());

        mockMvc.perform(post("/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("username", "alice")
                        .param("password", "password123"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"));
    }

    @Test
    void registerFormValidatesPasswordLength() throws Exception {
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("username", "alice")
                        .param("password", "short"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeHasFieldErrors("registerRequest", "password"));

        verify(userService, never()).register(any());
    }
}
