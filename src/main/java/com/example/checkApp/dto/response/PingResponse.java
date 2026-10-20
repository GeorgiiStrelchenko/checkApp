package com.example.checkApp.dto.response;

import com.example.checkApp.model.CheckStatus;

public record PingResponse(
        String url,
        CheckStatus status,
        long responseTimeMs,
        Integer httpCode,
        String errorMessage
) {
}
