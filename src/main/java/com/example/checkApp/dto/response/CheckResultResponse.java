package com.example.checkApp.dto.response;

import java.time.Instant;

import com.example.checkApp.model.CheckStatus;

/**
 * DTO одного результата проверки (строки истории) — отдаётся в
 * {@code GET /api/checks/{id}/results} и на странице чека.
 *
 * <p>{@code httpCode} и {@code errorMessage} — «зеркало» состояния проверки:
 * при UP обычно заполнен код и пуста ошибка, при DOWN — наоборот (но 500 от сервера
 * тоже UP с кодом 500: сайт отвечает, это его дело).</p>
 *
 * @param id              id записи истории
 * @param status          UP/DOWN
 * @param httpCode        HTTP-код ответа (null — соединения не было)
 * @param responseTimeMs  время отклика, мс (null — не измерено)
 * @param errorMessage    текст ошибки соединения (null — ошибки не было)
 * @param checkedAt       момент проверки
 */
public record CheckResultResponse(
        Long id,
        CheckStatus status,
        Integer httpCode,
        Long responseTimeMs,
        String errorMessage,
        Instant checkedAt
) {
}
