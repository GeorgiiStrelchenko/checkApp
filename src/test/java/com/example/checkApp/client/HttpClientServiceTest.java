package com.example.checkApp.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.ResourceAccessException;

class HttpClientServiceTest {

    private MockRestServiceServer server;
    private HttpClientService httpClientService;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        httpClientService = new HttpClientService(builder.build());
    }

    @Test
    void returnsHttpCodeOnSuccess() {
        server.expect(requestTo("https://example.com"))
                .andRespond(withSuccess("{\"ok\":true}", MediaType.APPLICATION_JSON));

        PingOutcome outcome = httpClientService.ping("https://example.com");

        assertThat(outcome.reachable()).isTrue();
        assertThat(outcome.httpCode()).isEqualTo(200);
        assertThat(outcome.errorMessage()).isNull();
        assertThat(outcome.responseTimeMs()).isGreaterThanOrEqualTo(0);
        server.verify();
    }

    @Test
    void returnsHttpCodeOnServerErrorWithoutException() {
        server.expect(requestTo("https://example.com"))
                .andRespond(withServerError());

        PingOutcome outcome = httpClientService.ping("https://example.com");

        assertThat(outcome.reachable()).isTrue();
        assertThat(outcome.httpCode()).isEqualTo(500);
        server.verify();
    }

    @Test
    void returnsErrorMessageWhenConnectionRefused() {
        server.expect(requestTo("https://unreachable.example.com"))
                .andRespond(withException(new IOException("Connection refused")));

        PingOutcome outcome = httpClientService.ping("https://unreachable.example.com");

        assertThat(outcome.reachable()).isFalse();
        assertThat(outcome.httpCode()).isNull();
        assertThat(outcome.errorMessage()).contains("Connection refused");
        server.verify();
    }

    @Test
    void wrapsResourceAccessExceptionForInvalidHost() {
        server.expect(requestTo("https://bad.example.com"))
                .andRespond(withException(new IOException("Name or service not known")));

        PingOutcome outcome = httpClientService.ping("https://bad.example.com");

        assertThat(outcome.errorMessage()).isNotNull();
        server.verify();
    }
}
