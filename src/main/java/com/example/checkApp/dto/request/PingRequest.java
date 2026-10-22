package com.example.checkApp.dto.request;

import jakarta.validation.constraints.NotBlank;

import org.hibernate.validator.constraints.URL;

/**
 * DTO тела {@code POST /api/ping} — одиночная проверка URL без создания чека.
 *
 * <p>{@code @URL} (Hibernate Validator) не даёт пустой строке или мусору дойти
 * до HTTP-клиента: кривой URL → сразу 400 Bad Request, без попытки соединения.</p>
 *
 * @param url полный URL для проверки, например {@code https://example.com}
 */
public record PingRequest(
        @NotBlank(message = "url must not be blank")
        @URL(message = "url must be a valid URL")
        String url
) {
}
