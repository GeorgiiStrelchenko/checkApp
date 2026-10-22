package com.example.checkApp.dto.response;

import com.example.checkApp.model.CheckStatus;

/**
 * DTO ответа одиночного пинга — {@code POST /api/ping}.
 *
 * <p>Отличается от результата истории ({@code CheckResultResponse}): здесь
 * возвращается <b>результат в реальном времени</b> без записи в БД —
 * URL эха своим, id нет.</p>
 *
 * @param url             что проверяли
 * @param status          UP/DOWN по итогу проверки
 * @param responseTimeMs  время отклика, мс
 * @param httpCode        код ответа (null — соединение не удалось)
 * @param errorMessage    текст ошибки (null — ошибки не было)
 */
public record PingResponse(
        String url,
        CheckStatus status,
        long responseTimeMs,
        Integer httpCode,
        String errorMessage
) {
}
