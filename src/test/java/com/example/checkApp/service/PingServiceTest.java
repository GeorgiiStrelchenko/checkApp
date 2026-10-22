package com.example.checkApp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.checkApp.client.HttpClientService;
import com.example.checkApp.client.PingOutcome;
import com.example.checkApp.dto.response.PingResponse;
import com.example.checkApp.model.CheckStatus;

/**
 * Unit-тесты PingService: проверяем «перевод» результата HTTP-клиента в ответ API.
 *
 * <p>Ключевые сценарии (HTTP-клиент замокан, реальной сети нет):</p>
 * <ul>
 *   <li>сервер ответил → статус UP;</li>
 *   <li>соединение не удалось → статус DOWN, текст ошибки доходит до ответа.</li>
 * </ul>
 *
 * <p>Паттерн: {@code @Mock} — зависимость-заглушка, {@code @InjectMocks} —
 * сервис, в который заглушка внедряется Mockito автоматически.</p>
 */
@ExtendWith(MockitoExtension.class)
class PingServiceTest {

    @Mock
    private HttpClientService httpClientService;

    @InjectMocks
    private PingService pingService;

    @Test
    void returnsUpWhenServerResponds() {
        when(httpClientService.ping("https://example.com"))
                .thenReturn(new PingOutcome(200, 42L, null));

        PingResponse response = pingService.ping("https://example.com");

        assertThat(response.status()).isEqualTo(CheckStatus.UP);
        assertThat(response.httpCode()).isEqualTo(200);
        assertThat(response.responseTimeMs()).isEqualTo(42L);
        assertThat(response.errorMessage()).isNull();
        assertThat(response.url()).isEqualTo("https://example.com");
    }

    @Test
    void returnsDownWhenConnectionFails() {
        when(httpClientService.ping("https://down.example.com"))
                .thenReturn(new PingOutcome(null, 5000L, "Connect timed out"));

        PingResponse response = pingService.ping("https://down.example.com");

        assertThat(response.status()).isEqualTo(CheckStatus.DOWN);
        assertThat(response.httpCode()).isNull();
        assertThat(response.errorMessage()).isEqualTo("Connect timed out");
    }
}
