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

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI checkAppOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("checkApp API")
                        .description("Website uptime monitoring API: checks, ping, history, auth")
                        .version("v1")
                        .contact(new Contact().name("checkApp").url("https://github.com/GeorgiiStrelchenko/checkApp")))
                .addServersItem(new Server().url("/").description("current host"))
                .components(new Components()
                        .addSecuritySchemes("session",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.APIKEY)
                                        .in(SecurityScheme.In.COOKIE)
                                        .name("JSESSIONID")
                                        .description("Session cookie obtained via POST /login")))
                .addSecurityItem(new SecurityRequirement().addList("session"));
    }
}
