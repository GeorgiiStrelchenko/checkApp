package com.example.checkApp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;

/**
 * Настройка документации OpenAPI 3.1 (springdoc-openapi).
 *
 * <p>springdoc сам сканирует контроллеры и собирает спецификацию по аннотациям
 * ({@code @Tag}, {@code @Operation}, DTO-классы), а этот бин {@code OpenAPI} добавляет
 * «шапку» документации: заголовок, описание, сервер и схему аутентификации.</p>
 *
 * <p>Результат доступен по адресам:</p>
 * <ul>
 *   <li>JSON-спецификация — {@code GET /v3/api-docs};</li>
 *   <li>Swagger UI — {@code GET /swagger-ui/index.html}.</li>
 * </ul>
 * Оба эндпоинта публичны (см. {@link SecurityConfig}), иначе документацию не открыть без логина.
 *
 * <p>UI-маршруты (Thymeleaf) в документацию не попадают: у {@link com.example.checkApp.controller.UiController}
 * стоит {@code @Hidden}.</p>
 */
@Configuration
public class OpenApiConfig {

    /**
     * Мета-описание всей API-документации.
     *
     * <p>Схема аутентификации здесь честная: API защищено session-based авторизацией
     * (не Bearer-токенами), поэтому описан cookie {@code JSESSIONID}, который появляется
     * после успешного {@code POST /login}. В Swagger UI это позволяет «пологиниться»
     * и дёргать API с интерактивной авторизацией.</p>
     */
    @Bean
    OpenAPI checkAppOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("checkApp API")
                        .description("Website uptime monitoring API: checks, ping, history, auth")
                        .version("v1")
                        .contact(new Contact().name("checkApp").url("https://github.com/GeorgiiStrelchenko/checkApp")))
                // сервер описываем как "/" (текущий хост) — документация работает и на localhost, и в Docker
                .addServersItem(new Server().url("/").description("current host"))
                .components(new Components()
                        // «вход» в API: cookie-сессия, полученная формой POST /login
                        .addSecuritySchemes("session",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.APIKEY)
                                        .in(SecurityScheme.In.COOKIE)
                                        .name("JSESSIONID")
                                        .description("Session cookie obtained via POST /login")))
                // требуем схему "session" для всех эндпоинтов по умолчанию (у публичных она просто не нужна)
                .addSecurityItem(new SecurityRequirement().addList("session"));
    }
}
