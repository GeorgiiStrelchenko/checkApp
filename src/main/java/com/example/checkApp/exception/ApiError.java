package com.example.checkApp.exception;

import java.time.Instant;
import java.util.Map;

/**
 * Единый формат JSON-тела ошибки для всех REST-ответов со статусом 4xx/5xx.
 *
 * <p>Вместо «Spring-HTML страницы ошибки» клиент API всегда получает предсказуемый JSON:</p>
 * <pre>
 * {
 *   "timestamp": "2026-10-21T18:00:00Z",
 *   "status": 400,
 *   "error": "Bad Request",
 *   "message": "Validation failed",
 *   "path": "/api/checks",
 *   "fieldErrors": { "url": "url must be a valid URL" }
 * }
 * </pre>
 *
 * <p>{@code fieldErrors} заполняется только при ошибках валидации — по нему UI подсвечивает
 * конкретные поля; для остальных ошибок там пустая карта.</p>
 *
 * @param timestamp   момент ошибки (UTC)
 * @param status      HTTP-код
 * @param error       каноническая фраза статуса («Not Found»)
 * @param message     понятное человеку описание причины
 * @param path        URI, на котором ошиблись
 * @param fieldErrors ошибки по полям (валидация), иначе пусто
 */
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> fieldErrors
) {
}
