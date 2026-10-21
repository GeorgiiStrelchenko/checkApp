package com.example.checkApp.integration;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.example.checkApp.entity.User;
import com.example.checkApp.repository.CheckRepository;
import com.example.checkApp.repository.CheckResultRepository;
import com.example.checkApp.repository.UserRepository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CheckRepository checkRepository;

    @Autowired
    private CheckResultRepository checkResultRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void cleanDatabase() {
        deleteAllData();
    }

    @AfterEach
    void cleanDatabaseAfterTest() {
        deleteAllData();
    }

    private void deleteAllData() {
        checkResultRepository.deleteAll();
        checkRepository.deleteAll();
        userRepository.deleteAll();
    }

    private void register(String username, String password) throws Exception {
        mockMvc.perform(post("/api/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void fullFlowRegisterLoginCreateCheck() throws Exception {
        register("alice", "password123");

        mockMvc.perform(formLogin("/login").user("alice").password("password123"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrlPattern("/**"));

        mockMvc.perform(post("/api/checks")
                        .with(user("alice"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"google\",\"url\":\"https://google.com\","
                                + "\"checkIntervalSeconds\":60,\"enabled\":true}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("google"));

        mockMvc.perform(get("/api/checks").with(user("alice")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("google"));
    }

    @Test
    void apiReturns401WithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/checks"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void duplicateRegistrationReturns409() throws Exception {
        register("bob", "password123");

        mockMvc.perform(post("/api/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"bob\",\"password\":\"password123\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void userCannotSeeForeignChecks() throws Exception {
        register("carol", "password123");
        register("dave", "password123");

        MvcResult created = mockMvc.perform(post("/api/checks")
                        .with(user("carol"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"carol-site\",\"url\":\"https://carol.com\","
                                + "\"checkIntervalSeconds\":60,\"enabled\":true}"))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode body = objectMapper.readTree(created.getResponse().getContentAsString());
        long checkId = body.get("id").asLong();

        mockMvc.perform(get("/api/checks/" + checkId).with(user("dave")))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/checks").with(user("dave")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        mockMvc.perform(get("/api/checks/" + checkId).with(user("carol")))
                .andExpect(status().isOk());
    }

    @Test
    void passwordIsStoredEncoded() throws Exception {
        register("erin", "password123");

        User erin = userRepository.findByUsername("erin").orElseThrow();
        assertThatPasswordIsBcrypt(erin.getPassword());
    }

    private static void assertThatPasswordIsBcrypt(String stored) {
        org.assertj.core.api.Assertions.assertThat(stored)
                .startsWith("$2a$")
                .isNotEqualTo("password123");
    }
}
