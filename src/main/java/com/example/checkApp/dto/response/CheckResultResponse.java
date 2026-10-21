package com.example.checkApp.dto.response;

import java.time.Instant;

import com.example.checkApp.model.CheckStatus;

public record CheckResultResponse(
        Long id,
        CheckStatus status,
        Integer httpCode,
        Long responseTimeMs,
        String errorMessage,
        Instant checkedAt
) {
}
