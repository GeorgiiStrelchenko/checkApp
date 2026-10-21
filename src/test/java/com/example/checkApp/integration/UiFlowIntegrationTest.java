package com.example.checkApp.integration;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.example.checkApp.repository.CheckRepository;
import com.example.checkApp.repository.CheckResultRepository;
import com.example.checkApp.repository.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UiFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CheckRepository checkRepository;

    @Autowired
    private CheckResultRepository checkResultRepository;

    @BeforeEach
    @AfterEach
    void cleanDatabase() {
        checkResultRepository.deleteAll();
        checkRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void registerLoginAndManageChecksThroughUi() throws Exception {
        // регистрация через форму
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("username", "henry")
                        .param("password", "password123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?registered"));

        // логин через форму
        mockMvc.perform(post("/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("username", "henry")
                        .param("password", "password123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));

        // второй пользователь существует в базе (для проверки чужого доступа)
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("username", "other")
                        .param("password", "password123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?registered"));

        // пустой дашборд
        mockMvc.perform(get("/").with(user("henry")))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"));

        // добавление чека формой
        mockMvc.perform(post("/checks")
                        .with(user("henry"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("name", "example")
                        .param("url", "https://example.com")
                        .param("checkIntervalSeconds", "60")
                        .param("enabled", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));

        // чек появился в базе и привязан к пользователю
        var checks = checkRepository.findAll();
        org.assertj.core.api.Assertions.assertThat(checks).hasSize(1);
        org.assertj.core.api.Assertions.assertThat(checks.get(0).getUser().getUsername()).isEqualTo("henry");

        // страница чека открывается
        mockMvc.perform(get("/checks/" + checks.get(0).getId()).with(user("henry")))
                .andExpect(status().isOk())
                .andExpect(view().name("check-detail"));

        // чужой пользователь не видит чек — редирект на дашборд
        mockMvc.perform(get("/checks/" + checks.get(0).getId()).with(user("other")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));

        // выключение чека формой
        mockMvc.perform(post("/checks/" + checks.get(0).getId() + "/toggle")
                        .with(user("henry"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
        org.assertj.core.api.Assertions
                .assertThat(checkRepository.findAll().get(0).isEnabled()).isFalse();

        // удаление чека формой
        mockMvc.perform(post("/checks/" + checks.get(0).getId() + "/delete")
                        .with(user("henry"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
        org.assertj.core.api.Assertions.assertThat(checkRepository.findAll()).isEmpty();
    }

    @Test
    void anonymousIsRedirectedToLogin() throws Exception {
        mockMvc.perform(get("/").accept(MediaType.TEXT_HTML))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPatternHasLogin());
    }

    private static org.springframework.test.web.servlet.ResultMatcher redirectedUrlPatternHasLogin() {
        return result -> {
            String location = result.getResponse().getRedirectedUrl();
            org.assertj.core.api.Assertions.assertThat(location).contains("/login");
        };
    }
}
