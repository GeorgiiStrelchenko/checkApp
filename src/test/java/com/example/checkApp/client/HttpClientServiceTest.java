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

/**
 * Unit-тесты HTTP-клиента мониторинга — <b>без реальной сети</b>.
 *
 * <p>Вместо настоящего сервера используется {@link MockRestServiceServer} —
 * встроенный мок для RestClient: тест заранее описывает «ожидания»
 * ({@code server.expect(requestTo(...)).andRespond(...)}), а после выполнения
 * {@code server.verify()} сверяет, что всё прошло как задумано.</p>
 *
 * <p>Проверяем три сценария, критичных для мониторинга:</p>
 * <ol>
 *   <li>200 OK → reachable, код 200, ошибки нет;</li>
 *   <li>500 Server Error → reachable всё равно true (сайт ответил!),
 *       исключение НЕ бросается — это принципиальное поведение;</li>
 *   <li>сеть недоступна (IOException) → reachable false, текст ошибки сохранён.</li>
 * </ol>
 */
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
