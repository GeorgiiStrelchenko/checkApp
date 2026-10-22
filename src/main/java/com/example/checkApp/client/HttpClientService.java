package com.example.checkApp.client;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * HTTP-клиент мониторинга: умеет «пинговать» один URL и превращать результат в {@link PingOutcome}.
 *
 * <p>Ключевая идея: <b>любой</b> результат пинга — это не исключение, а данные.
 * Недоступный сайт, таймаут, DNS-ошибка — всё это возвращается как
 * {@code PingOutcome} с {@code errorMessage}, а не пробрасывается наверх.
 * Планировщику и сервису проще работать с «упавшим результатом», чем ловить исключения.</p>
 *
 * <p>Используется из {@link com.example.checkApp.service.PingService} (одиночный пинг API)
 * и {@link com.example.checkApp.service.CheckRunService} (фоновые проверки).</p>
 */
@Service
public class HttpClientService {

    private final RestClient restClient;

    /**
     * Конструкторная инъекция: Spring передаёт настроенный бин
     * из {@link com.example.checkApp.config.RestClientConfig} (с таймаутами 5с/10с).
     */
    public HttpClientService(RestClient restClient) {
        this.restClient = restClient;
    }

    /**
     * Выполняет GET-запрос к {@code url} и замеряет время отклика.
     *
     * <p>Семантика возвращаемых значений:</p>
     * <ul>
     *   <li>сервер ответил (любой код, даже 404/500) → {@code httpCode} заполнен, {@code errorMessage == null}
     *       — сайт <b>доступен</b>, просто вернул не-200;</li>
     *   <li>соединение не удалось / таймаут / DNS и т.п. → {@code httpCode == null},
     *       {@code errorMessage} = текст ошибки — сайт <b>недоступен</b>.</li>
     * </ul>
     *
     * <p>{@code exchange(...)} вместо {@code retrieve()}: нам нужен только код ответа,
     * а {@code retrieve()} бросил бы исключение на не-2xx статусах, что помешало бы
     * считать «404 = сайт жив, но страница удалена».</p>
     *
     * @param url полный URL для проверки (например, https://google.com)
     * @return результат пинга; исключения не вылетают наружу
     */
    public PingOutcome ping(String url) {
        // Замеряем с nanoTime — монотонные часы, не подвержены переводу системного времени
        long start = System.nanoTime();
        try {
            Integer httpCode = restClient.get()
                    .uri(url)
                    // перехватываем ответ на уровне exchange: возвращаем только статус, тело не читаем
                    .exchange((request, response) -> response.getStatusCode().value());
            return new PingOutcome(httpCode, elapsedMs(start), null);
        } catch (RestClientException e) {
            // Все сетевые ошибки (connect timeout, read timeout, UnknownHost, SSL...) сюда.
            // e.getMessage() потом уходит в чек (обрезается до 2048 символов) и в историю результатов.
            return new PingOutcome(null, elapsedMs(start), e.getMessage());
        }
    }

    /** Переводит наносекунды (от {@code start}) в целые миллисекунды. */
    private long elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }
}
