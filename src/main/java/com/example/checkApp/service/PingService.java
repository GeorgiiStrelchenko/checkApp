package com.example.checkApp.service;

import org.springframework.stereotype.Service;

import com.example.checkApp.client.HttpClientService;
import com.example.checkApp.client.PingOutcome;
import com.example.checkApp.dto.response.PingResponse;
import com.example.checkApp.model.CheckStatus;

/**
 * Одиночный пинг «на запрос»: используется REST-эндпоинтом {@code POST /api/ping}.
 *
 * <p>Роль этого сервиса — «переупаковать» сырой результат HTTP-клиента
 * ({@link PingOutcome}) в формат ответа API ({@link PingResponse}) и принять
 * решение <b>UP или DOWN</b>:</p>
 * <ul>
 *   <li>сервер ответил <i>любым</i> кодом (200, 404, 500...) → <b>UP</b>:
 *       сайт доступен, код ответа — его дело;</li>
 *   <li>соединение не удалось (таймаут, DNS, отказ соединения) → <b>DOWN</b>.</li>
 * </ul>
 *
 * <p>Это отдельный сервис (а не просто метод HttpClientService), потому что «пинг с сохранением
 * в историю» и «пинг без сохранения» — разные задачи: этот ничего не пишет в БД.</p>
 */
@Service
public class PingService {

    private final HttpClientService httpClientService;

    public PingService(HttpClientService httpClientService) {
        this.httpClientService = httpClientService;
    }

    /**
     * Пингует URL и возвращает результат в формате API.
     *
     * @param url полный URL (валидируется аннотацией {@code @URL} в DTO до вызова)
     * @return DTO с URL, статусом UP/DOWN, временем отклика, кодом и текстом ошибки
     */
    public PingResponse ping(String url) {
        PingOutcome outcome = httpClientService.ping(url);
        // reachable() = не было ошибки соединения; не-2xx коды всё равно считаются UP
        CheckStatus status = outcome.reachable() ? CheckStatus.UP : CheckStatus.DOWN;
        return new PingResponse(
                url,
                status,
                outcome.responseTimeMs(),
                outcome.httpCode(),
                outcome.errorMessage()
        );
    }
}
