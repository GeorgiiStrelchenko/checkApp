package com.example.checkApp.service;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.checkApp.dto.response.PingResponse;
import com.example.checkApp.entity.Check;
import com.example.checkApp.entity.CheckResult;
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

    @Transactional
    public CheckResult recordResult(Check check, PingResponse ping) {
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
        return result;
    }

    private static String truncate(String value) {
        if (value == null || value.length() <= MAX_ERROR_MESSAGE_LENGTH) {
            return value;
        }
        return value.substring(0, MAX_ERROR_MESSAGE_LENGTH);
    }
}
