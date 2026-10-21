package com.example.checkApp.dto.response;

import java.time.Instant;

import com.example.checkApp.model.CheckStatus;

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
