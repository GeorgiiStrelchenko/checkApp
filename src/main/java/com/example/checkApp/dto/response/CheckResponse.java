package com.example.checkApp.dto.response;

import java.time.Instant;

import com.example.checkApp.model.CheckStatus;

/**
 * DTO чека во внешнем мире (JSON API и данные для шаблонов).
 *
 * <p>Сознательно <b>не</b> включает {@code user}: владелец и так известен по сессии,
 * отдавать его в JSON (id, имя) незачем — меньше данных наружу.</p>
 *
 * <p>Поля {@code lastStatus}/{@code lastCheckedAt} — «последнее известное состояние»,
 * обновляемое планировщиком; {@code null} у статуса означает «чек ещё не проверялся».</p>
 *
 * @param id                  id чека
 * @param name                имя
 * @param url                 проверяемый URL
 * @param checkIntervalSeconds интервал проверки, с
 * @param enabled             включён ли чек
 * @param lastStatus          последний статус (UP/DOWN/null)
 * @param lastCheckedAt       момент последней проверки (null — ещё не проверялся)
 * @param createdAt           момент создания
 */
public record CheckResponse(
        Long id,
        String name,
        String url,
        int checkIntervalSeconds,
        boolean enabled,
        CheckStatus lastStatus,
        Instant lastCheckedAt,
        Instant createdAt
) {
}
