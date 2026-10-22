package com.example.checkApp.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.example.checkApp.entity.Check;
import com.example.checkApp.model.CheckStatus;

import lombok.extern.slf4j.Slf4j;

/**
 * Реализация {@link AlertNotifier} через Telegram Bot API.
 *
 * <p><b>Два режима работы</b> (переключаются конфигурацией, а не кодом):</p>
 * <ul>
 *   <li>{@code app.telegram.bot-token} и {@code app.telegram.chat-id} <b>заполнены</b> —
 *       реальный HTTP-запрос {@code POST https://api.telegram.org/bot<token>/sendMessage}:</li>
 *   <li>хотя бы один пустой (значение по умолчанию) — <b>режим заглушки</b>: сообщение
 *       пишется в лог с префиксом {@code [telegram-stub]}. Это удобно для разработки
 *       и CI: секреты не нужны, проект запускается «из коробки».</li>
 * </ul>
 *
 * <p>Ошибки отправки <b>не валят</b> проверку: телеграм лёг — мониторинг обязан продолжать
 * работать, поэтому исключение лишь логируется.</p>
 *
 * <p>Токен/чат читаем через {@code @Value} из application.yml / переменных окружения
 * ({@code TELEGRAM_BOT_TOKEN}, {@code TELEGRAM_CHAT_ID}).</p>
 */
@Service
@Slf4j
public class TelegramAlertService implements AlertNotifier {

    /** Шаблон URL Bot API; {token} подставляется через RestClient.uri(template, token). */
    private static final String TELEGRAM_API_URL = "https://api.telegram.org/bot{token}/sendMessage";

    private final RestClient restClient;
    private final String botToken;
    private final String chatId;

    /**
     * Свой RestClient собираем из общего билдера (с таймаутами из RestClientConfig),
     * потому что Telegram — внешний сервис, и «зависший» запрос тоже должен иметь предел.
     *
     * @param botToken токен бота; пусто = режим заглушки
     * @param chatId   id чата для уведомлений; пусто = режим заглушки
     */
    public TelegramAlertService(RestClient.Builder restClientBuilder,
            @Value("${app.telegram.bot-token:}") String botToken,
            @Value("${app.telegram.chat-id:}") String chatId) {
        this.restClient = restClientBuilder.build();
        this.botToken = botToken;
        this.chatId = chatId;
    }

    /**
     * Отправляет сообщение о смене статуса — либо в Telegram, либо (в режиме заглушки) в лог.
     *
     * <p>Вызывается только при смене статуса — см. {@link CheckRunService}.</p>
     */
    @Override
    public void notifyStatusChange(Check check, CheckStatus previousStatus, CheckStatus currentStatus) {
        String message = formatMessage(check, previousStatus, currentStatus);

        // Секретов нет → ничего не отправляем наружу, но событие видно в логе
        if (botToken.isBlank() || chatId.isBlank()) {
            log.warn("[telegram-stub] {}", message);
            return;
        }

        try {
            restClient.post()
                    .uri(TELEGRAM_API_URL, botToken)
                    // тело — record с полями под JSON Telegram API: {"chat_id": "...", "text": "..."}
                    .body(new TelegramMessage(chatId, message))
                    .retrieve()
                    .toBodilessEntity();
            log.info("Telegram alert sent: {}", message);
        } catch (RestClientException e) {
            // не пробрасываем: упавший телеграм не должен ронять мониторинг
            log.error("Failed to send telegram alert '{}': {}", message, e.getMessage());
        }
    }

    /**
     * Формат сообщения: эмодзи + статус + имя и URL чека.
     *
     * <p>Текст зависит от направления перехода: «упал» или «восстановился».</p>
     */
    private static String formatMessage(Check check, CheckStatus previousStatus, CheckStatus currentStatus) {
        String location = "'%s' (%s)".formatted(check.getName(), check.getUrl());
        if (currentStatus == CheckStatus.DOWN) {
            return "🔴 DOWN: %s".formatted(location);
        }
        return "🟢 RECOVERED: %s (was %s)".formatted(location, previousStatus);
    }

    /**
     * Внутренний DTO под JSON Bot API. Имена полей {@code chat_id}/{@code text}
     * обязаны совпадать с документацией Telegram — они и станут ключами JSON.
     */
    private record TelegramMessage(String chat_id, String text) {
    }
}
