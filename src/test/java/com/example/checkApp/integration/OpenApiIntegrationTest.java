package com.example.checkApp.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import com.example.checkApp.PostgresTestSupport;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/**
 * Интеграционные тесты документации: springdoc отдаёт спецификацию, Swagger UI открыт
 * без аутентификации, UI-маршруты в API-документацию не попадают.
 *
 * <p>{@code @SpringBootTest} + MockMvc + реальная БД (Testcontainers) — потому что
 * генерация OpenAPI в springdoc частично опирается на реально поднятые бины/контроллеры.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
class OpenApiIntegrationTest extends PostgresTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void apiDocsArePublicAndContainEndpoints() throws Exception {
        String body = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").value("3.1.0"))
                .andExpect(jsonPath("$.info.title").value("checkApp API"))
                .andReturn().getResponse().getContentAsString();

        var paths = objectMapper.readValue(body, new TypeReference<java.util.Map<String, Object>>() {
        }).get("paths");
        var raw = objectMapper.writeValueAsString(paths);
        org.assertj.core.api.Assertions.assertThat(raw)
                .contains("/api/checks")
                .contains("/api/ping")
                .contains("/api/register");
        // UI-маршруты не должны попадать в API-документацию
        org.assertj.core.api.Assertions.assertThat(raw).doesNotContain("\"/login\"");
    }

    @Test
    void swaggerUiIsPublic() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }

    @Test
    void apiDocsRequireNoAuthentication() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk());
    }
}
