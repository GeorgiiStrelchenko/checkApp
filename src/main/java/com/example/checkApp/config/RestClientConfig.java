package com.example.checkApp.config;

import java.net.http.HttpClient;
import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Настройка HTTP-клиента {@link RestClient}, которым сервис пингует проверяемые сайты.
 *
 * <p>{@code RestClient} — современный (Spring 6.1+) синхронный HTTP-клиент Spring.
 * Здесь мы оборачиваем встроенный {@link java.net.http.HttpClient} из JDK и задаём таймауты,
 * потому что для uptime-мониторинга таймауты критичны: «медленный» сайт должен считаться DOWN,
 * а не висеть до бесконечности и блокировать планировщик.</p>
 */
@Configuration
public class RestClientConfig {

    /** Таймаут на установление TCP-соединения: 5 секунд. */
    public static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);

    /** Таймаут на чтение ответа (после отправки запроса): 10 секунд. */
    public static final Duration READ_TIMEOUT = Duration.ofSeconds(10);

    /**
     * Фабрика запросов с таймаутами + готовый билдер {@code RestClient}.
     *
     * <p>Bean-билдера регистрируем отдельно, чтобы другие бины могли доращивать
     * клиент своими настройками (например, добавить логгер), а сам {@link RestClient}
     * получать уже собранным.</p>
     */
    @Bean
    RestClient.Builder restClientBuilder() {
        // HttpClient JDK: connectTimeout — время на соединение
        HttpClient jdkClient = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .build();
        // JdkClientHttpRequestFactory — мост между Spring RestClient и java.net.http.HttpClient.
        // readTimeout — сколько ждать тела ответа после соединения.
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(jdkClient);
        requestFactory.setReadTimeout(READ_TIMEOUT);
        return RestClient.builder()
                .requestFactory(requestFactory);
    }

    /**
     * Готовый клиент для пинга. Его инжектит {@link com.example.checkApp.client.HttpClientService}.
     */
    @Bean
    RestClient restClient(RestClient.Builder builder) {
        return builder.build();
    }
}
