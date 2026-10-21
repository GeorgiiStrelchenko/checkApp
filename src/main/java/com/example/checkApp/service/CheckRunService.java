package com.example.checkApp.service;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.checkApp.dto.response.PingResponse;
import com.example.checkApp.entity.Check;
import com.example.checkApp.entity.CheckResult;
import com.example.checkApp.model.CheckStatus;
import com.example.checkApp.repository.CheckRepository;
import com.example.checkApp.repository.CheckResultRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class CheckRunService {

    private static final int MAX_ERROR_MESSAGE_LENGTH = 2048;

    private final CheckResultRepository checkResultRepository;
    private final CheckRepository checkRepository;
    private final AlertNotifier alertNotifier;

    @Transactional
    public CheckResult recordResult(Check check, PingResponse ping) {
        CheckStatus previousStatus = check.getLastStatus();

        CheckResult result = new CheckResult();
        result.setCheck(check);
        result.setStatus(ping.status());
        result.setHttpCode(ping.httpCode());
        result.setResponseTimeMs(ping.responseTimeMs());
        result.setErrorMessage(truncate(ping.errorMessage()));
        checkResultRepository.save(result);

        check.setLastStatus(ping.status());
        check.setLastCheckedAt(Instant.now());
        checkRepository.save(check);

        log.info("Check '{}' ({}) -> {} ({} ms, code {})",
                check.getName(), check.getUrl(), ping.status(), ping.responseTimeMs(), ping.httpCode());

        if (isStatusChange(previousStatus, ping.status())) {
            alertNotifier.notifyStatusChange(check, previousStatus, ping.status());
        }
        return result;
    }

    private static boolean isStatusChange(CheckStatus previousStatus, CheckStatus currentStatus) {
        if (previousStatus == null) {
            return currentStatus == CheckStatus.DOWN;
        }
        return previousStatus != currentStatus;
    }

    private static String truncate(String value) {
        if (value == null || value.length() <= MAX_ERROR_MESSAGE_LENGTH) {
            return value;
        }
        return value.substring(0, MAX_ERROR_MESSAGE_LENGTH);
    }
}
