package com.example.checkApp.client;

public record PingOutcome(
        Integer httpCode,
        long responseTimeMs,
        String errorMessage
) {
    public boolean reachable() {
        return errorMessage == null;
    }
}
