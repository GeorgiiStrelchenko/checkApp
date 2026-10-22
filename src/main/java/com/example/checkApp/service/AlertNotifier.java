package com.example.checkApp.service;

import com.example.checkApp.entity.Check;
import com.example.checkApp.model.CheckStatus;

/**
 * Интерфейс оповещения о смене статуса чека (UP ↔ DOWN).
 *
 * <p><b>Зачем интерфейс:</b> {@link com.example.checkApp.service.CheckRunService} не знает,
 * <i>как</i> доставляется алерт — ему достаточно вызвать метод. Это классический паттерн
 * «ширина вместо глубины»: к проекту можно подключить e-mail, Slack или webhook,
 * добавив новый {@code implements AlertNotifier}, не меняя ядро мониторинга.</p>
 *
 * <p><b>Зачем в тестах:</b> единственная реализация в проекте — {@link TelegramAlertService},
 * но в тестах вместо неё подставляется Mockito-заглушка ({@code @MockitoBean AlertNotifier})
 * и проверяется «был ли вызван notifyStatusChange» — так тест не шлёт настоящие запросы
 * в Telegram.</p>
 */
public interface AlertNotifier {

    /**
     * Вызывается <b>только при смене</b> статуса (логика — в CheckRunService).
     *
     * @param check          чек, у которого сменился статус
     * @param previousStatus статус до проверки; {@code null}, если это была первая проверка чека
     * @param currentStatus  новый статус
     */
    void notifyStatusChange(Check check, CheckStatus previousStatus, CheckStatus currentStatus);
}
