package com.example.checkApp.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.example.checkApp.entity.Check;
import com.example.checkApp.model.CheckStatus;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class TelegramAlertService implements AlertNotifier {

    private static final String TELEGRAM_API_URL = "https://api.telegram.org/bot{token}/sendMessage";

    private final RestClient restClient;
    private final String botToken;
    private final String chatId;

    public TelegramAlertService(RestClient.Builder restClientBuilder,
            @Value("${app.telegram.bot-token:}") String botToken,
            @Value("${app.telegram.chat-id:}") String chatId) {
        this.restClient = restClientBuilder.build();
        this.botToken = botToken;
        this.chatId = chatId;
    }

    @Override
    public void notifyStatusChange(Check check, CheckStatus previousStatus, CheckStatus currentStatus) {
        String message = formatMessage(check, previousStatus, currentStatus);

        if (botToken.isBlank() || chatId.isBlank()) {
            log.warn("[telegram-stub] {}", message);
            return;
        }

        try {
            restClient.post()
                    .uri(TELEGRAM_API_URL, botToken)
                    .body(new TelegramMessage(chatId, message))
                    .retrieve()
                    .toBodilessEntity();
            log.info("Telegram alert sent: {}", message);
        } catch (RestClientException e) {
            log.error("Failed to send telegram alert '{}': {}", message, e.getMessage());
        }
    }

    private static String formatMessage(Check check, CheckStatus previousStatus, CheckStatus currentStatus) {
        String location = "'%s' (%s)".formatted(check.getName(), check.getUrl());
        if (currentStatus == CheckStatus.DOWN) {
            return "🔴 DOWN: %s".formatted(location);
        }
        return "🟢 RECOVERED: %s (was %s)".formatted(location, previousStatus);
    }

    private record TelegramMessage(String chat_id, String text) {
    }
}
