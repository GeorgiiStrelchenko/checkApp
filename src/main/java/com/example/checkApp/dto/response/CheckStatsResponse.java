package com.example.checkApp.dto.response;

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
