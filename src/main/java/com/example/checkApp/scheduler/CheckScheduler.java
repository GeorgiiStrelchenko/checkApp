package com.example.checkApp.scheduler;

import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.example.checkApp.dto.response.PingResponse;
import com.example.checkApp.entity.Check;
import com.example.checkApp.repository.CheckRepository;
import com.example.checkApp.service.CheckRunService;
import com.example.checkApp.service.PingService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class CheckScheduler {

    private final CheckRepository checkRepository;
    private final PingService pingService;
    private final CheckRunService checkRunService;

    @Value("${app.scheduler.enabled:true}")
    private boolean schedulerEnabled;

    @Scheduled(fixedDelayString = "${app.scheduler.tick-ms:10000}")
    public void tick() {
        if (!schedulerEnabled) {
            return;
        }
        Instant now = Instant.now();
        for (Check check : checkRepository.findByEnabledTrue()) {
            if (isDue(check, now)) {
                runSingle(check);
            }
        }
    }

    void runSingle(Check check) {
        try {
            PingResponse ping = pingService.ping(check.getUrl());
            checkRunService.recordResult(check, ping);
        } catch (Exception e) {
            log.error("Unexpected error while checking '{}' ({}): {}", check.getName(), check.getUrl(), e.getMessage());
        }
    }

    static boolean isDue(Check check, Instant now) {
        Instant last = check.getLastCheckedAt();
        if (last == null) {
            return true;
        }
        return !last.plusSeconds(check.getCheckIntervalSeconds()).isAfter(now);
    }
}
