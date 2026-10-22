package com.example.checkApp.dto.response;

/**
 * DTO uptime-статистики за период — ответ {@code GET /api/checks/{id}/stats?hours=24}.
 *
 * <p>Отдельный DTO вместо «собери из истории сам»: агрегаты считаются один раз
 * на сервере (см. {@code CheckHistoryService.getStats}), клиент получает готовое.</p>
 *
 * <p>Соглашения по null: {@code avgResponseTimeMs}/{@code maxResponseTimeMs} —
 * {@code null}, если в окне не было ни одного измеренного времени отклика
 * (например, все проверки упали на DNS): <b>null ≠ 0</b> — «нет данных» и «0 мс»
 * это разные вещи.</p>
 *
 * @param checkId            чек, к которому относится статистика
 * @param periodHours        реально применённое окно в часах
 * @param totalChecks        проверок в окне
 * @param upCount            из них UP
 * @param downCount          из них DOWN (totalChecks - upCount)
 * @param uptimePercent      процент UP, 2 знака после запятой; 100.0, если проверок нет
 * @param avgResponseTimeMs  среднее время отклика по успешным замерам (null — нет данных)
 * @param maxResponseTimeMs  максимальное время отклика (null — нет данных)
 */
public record CheckStatsResponse(
        Long checkId,
        int periodHours,
        long totalChecks,
        long upCount,
        long downCount,
        double uptimePercent,
        Long avgResponseTimeMs,
        Long maxResponseTimeMs
) {
}
