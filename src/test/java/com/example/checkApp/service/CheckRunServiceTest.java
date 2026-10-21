package com.example.checkApp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.example.checkApp.dto.response.PingResponse;
import com.example.checkApp.entity.Check;
import com.example.checkApp.entity.CheckResult;
import com.example.checkApp.model.CheckStatus;
import com.example.checkApp.repository.CheckRepository;
import com.example.checkApp.repository.CheckResultRepository;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(CheckRunService.class)
class CheckRunServiceTest {

    @Autowired
    private CheckRunService checkRunService;

    @Autowired
    private CheckRepository checkRepository;

    @Autowired
    private CheckResultRepository checkResultRepository;

    @MockitoBean
    private AlertNotifier alertNotifier;

    private Check savedCheck() {
        Check check = new Check();
        check.setName("google");
        check.setUrl("https://google.com");
        return checkRepository.saveAndFlush(check);
    }

    @Test
    void recordsUpResultAndUpdatesCheck() {
        Check check = savedCheck();

        checkRunService.recordResult(check,
                new PingResponse("https://google.com", CheckStatus.UP, 42L, 200, null));

        Check reloaded = checkRepository.findById(check.getId()).orElseThrow();
        assertThat(reloaded.getLastStatus()).isEqualTo(CheckStatus.UP);
        assertThat(reloaded.getLastCheckedAt()).isNotNull();

        CheckResult result = checkResultRepository.findTop20ByCheckIdOrderByCheckedAtDesc(check.getId()).get(0);
        assertThat(result.getStatus()).isEqualTo(CheckStatus.UP);
        assertThat(result.getHttpCode()).isEqualTo(200);
        assertThat(result.getResponseTimeMs()).isEqualTo(42L);
    }

    @Test
    void recordsDownResultWithErrorMessage() {
        Check check = savedCheck();

        checkRunService.recordResult(check,
                new PingResponse("https://google.com", CheckStatus.DOWN, 5000L, null, "Connect timed out"));

        CheckResult result = checkResultRepository.findTop20ByCheckIdOrderByCheckedAtDesc(check.getId()).get(0);
        assertThat(result.getStatus()).isEqualTo(CheckStatus.DOWN);
        assertThat(result.getHttpCode()).isNull();
        assertThat(result.getErrorMessage()).isEqualTo("Connect timed out");
    }

    @Test
    void truncatesLongErrorMessage() {
        Check check = savedCheck();
        String longError = "x".repeat(3000);

        checkRunService.recordResult(check,
                new PingResponse("https://google.com", CheckStatus.DOWN, 1L, null, longError));

        CheckResult result = checkResultRepository.findTop20ByCheckIdOrderByCheckedAtDesc(check.getId()).get(0);
        assertThat(result.getErrorMessage()).hasSize(2048);
    }

    @Test
    void alertsOnFirstDownResult() {
        Check check = savedCheck();

        checkRunService.recordResult(check,
                new PingResponse("https://google.com", CheckStatus.DOWN, 5000L, null, "timeout"));

        verify(alertNotifier).notifyStatusChange(eq(check), eq(null), eq(CheckStatus.DOWN));
    }

    @Test
    void doesNotAlertOnFirstUpResult() {
        Check check = savedCheck();

        checkRunService.recordResult(check,
                new PingResponse("https://google.com", CheckStatus.UP, 42L, 200, null));

        verify(alertNotifier, never()).notifyStatusChange(any(), any(), any());
    }

    @Test
    void alertsWhenUpChangesToDown() {
        Check check = savedCheck();
        check.setLastStatus(CheckStatus.UP);

        checkRunService.recordResult(check,
                new PingResponse("https://google.com", CheckStatus.DOWN, 5000L, null, "timeout"));

        verify(alertNotifier).notifyStatusChange(eq(check), eq(CheckStatus.UP), eq(CheckStatus.DOWN));
    }

    @Test
    void alertsWhenDownRecoversToUp() {
        Check check = savedCheck();
        check.setLastStatus(CheckStatus.DOWN);

        checkRunService.recordResult(check,
                new PingResponse("https://google.com", CheckStatus.UP, 42L, 200, null));

        verify(alertNotifier).notifyStatusChange(eq(check), eq(CheckStatus.DOWN), eq(CheckStatus.UP));
    }

    @Test
    void doesNotAlertWhenStatusStaysTheSame() {
        Check check = savedCheck();
        check.setLastStatus(CheckStatus.UP);

        checkRunService.recordResult(check,
                new PingResponse("https://google.com", CheckStatus.UP, 42L, 200, null));

        verify(alertNotifier, never()).notifyStatusChange(any(), any(), any());
    }
}
