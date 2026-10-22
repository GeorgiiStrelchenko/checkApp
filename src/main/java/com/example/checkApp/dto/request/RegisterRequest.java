package com.example.checkApp.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO регистрации — используется и REST-контроллером ({@code POST /api/register}),
 * и веб-формой ({@code POST /register}).
 *
 * <p>Пароль валидируем на длину 8..72: меньше 8 — слабо, больше 72 — ограничение
 * BCrypt (он обрабатывает первые 72 байта; длиннее имеет смысла не хранить).
 * <b>Хэшируется</b> в {@code UserService}, сюда попадает «сырой» пароль.</p>
 *
 * @param username логин: непустой, до 255 символов (уникальность проверяет сервис + индекс БД)
 * @param password пароль: непустой, 8..72 символа
 */
public record RegisterRequest(
        @NotBlank(message = "username is required")
        @Size(max = 255)
        String username,

        @NotBlank(message = "password is required")
        @Size(min = 8, max = 72, message = "password must be between 8 and 72 characters")
        String password) {
}
