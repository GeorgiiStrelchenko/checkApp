package com.example.checkApp.dto.request;

import org.hibernate.validator.constraints.URL;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO на создание/обновление чека — приходит в теле {@code POST/PUT /api/checks}
 * и в форме {@code POST /checks} (веб-интерфейс использует тот же record).
 *
 * <p><b>Валидация здесь — часть контракта API:</b> все ограничения описаны аннотациями
 * Bean Validation, Spring применяет их из-за {@code @Valid} в контроллере и при ошибке
 * отдаёт 400 с перечнем полей (см. {@code GlobalExceptionHandler.handleValidation}).</p>
 *
 * <p>Record вместо класса: DTO неизменяем, компактен, все поля очевидны.
 * Сообщения об ошибках пишем явно — их видит клиент API.</p>
 *
 * @param name                имя чека: непустое, до 255 символов (колонка в БД)
 * @param url                 проверяемый URL: непустой, корректный (проверка {@code @URL}),
 *                            до 2048 символов
 * @param checkIntervalSeconds интервал проверки: обязателен, 10..86400 секунд
 *                            (минимум 10 с — чтобы не DoSить чужой сайт и сам планировщик)
 * @param enabled             включён ли чек: обязателен (null недопустим, но это булево «да/нет»)
 */
public record CheckRequest(
        @NotBlank(message = "name must not be blank")
        @Size(max = 255, message = "name must be at most 255 characters")
        String name,

        @NotBlank(message = "url must not be blank")
        @URL(message = "url must be a valid URL")
        @Size(max = 2048, message = "url must be at most 2048 characters")
        String url,

        @NotNull(message = "checkIntervalSeconds must not be null")
        @Min(value = 10, message = "checkIntervalSeconds must be at least 10")
        @Max(value = 86400, message = "checkIntervalSeconds must be at most 86400")
        Integer checkIntervalSeconds,

        @NotNull(message = "enabled must not be null")
        Boolean enabled
) {
}
