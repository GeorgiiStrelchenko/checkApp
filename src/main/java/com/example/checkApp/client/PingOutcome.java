package com.example.checkApp.client;

/**
 * Результат одного HTTP-пинга: код ответа, время отклика и (опционально) текст ошибки.
 *
 * <p>Java record — неизменяемый value-объект: поля фиксируются в конструкторе,
 * геттеры генерируются автоматически как {@code httpCode()}, {@code responseTimeMs()},
 * {@code errorMessage()} (без приставки {@code get}).</p>
 *
 * <p>Инвариант: либо {@code httpCode} != null (сайт ответил), либо {@code errorMessage} != null
 * (сайт недоступен). Обе null быть не должно — это гарантирует
 * {@link com.example.checkApp.client.HttpClientService}.</p>
 *
 * @param httpCode        HTTP-код ответа сервера; {@code null}, если соединение не удалось
 * @param responseTimeMs  время отклика в миллисекундах (всегда заполнено, даже при ошибке)
 * @param errorMessage    текст сетевой ошибки; {@code null}, если сервер ответил
 */
public record PingOutcome(
        Integer httpCode,
        long responseTimeMs,
        String errorMessage
) {

    /**
     * Достижим ли сайт: ошибки соединения не было.
     *
     * <p>Обратите внимание: «доступен» ≠ «вернул 200». Сайт с 500-й ошибкой
     * формально достижим — считать его DOWN или UP решает вызывающий код
     * (см. {@link com.example.checkApp.service.PingService}).</p>
     */
    public boolean reachable() {
        return errorMessage == null;
    }
}
