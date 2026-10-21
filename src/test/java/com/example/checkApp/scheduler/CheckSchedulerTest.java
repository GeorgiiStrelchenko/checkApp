package com.example.checkApp.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.checkApp.dto.response.PingResponse;
import com.example.checkApp.entity.Check;
import com.example.checkApp.model.CheckStatus;
import com.example.checkApp.repository.CheckRepository;
import com.example.checkApp.service.CheckRunService;
import com.example.checkApp.service.PingService;

@ExtendWith(MockitoExtension.class)
class CheckSchedulerTest {

    @Mock
    private CheckRepository checkRepository;

    @Mock
    private PingService pingService;

    @Mock
    private CheckRunService checkRunService;

    @InjectMocks
    private CheckScheduler scheduler;

    private Check checkWithInterval(int intervalSeconds, Instant lastCheckedAt) {
        Check check = new Check();
        check.setName("google");
        check.setUrl("https://google.com");
        check.setCheckIntervalSeconds(intervalSeconds);
        check.setLastCheckedAt(lastCheckedAt);
        return check;
    }

    @Test
    void isDueWhenNeverChecked() {
        assertThat(CheckScheduler.isDue(checkWithInterval(60, null), Instant.now())).isTrue();
    }

    @Test
    void isNotDueWhenIntervalNotElapsed() {
        Instant now = Instant.now();
        Check check = checkWithInterval(60, now.minus(30, ChronoUnit.SECONDS));

        assertThat(CheckScheduler.isDue(check, now)).isFalse();
    }

    @Test
    void isDueWhenIntervalElapsed() {
        Instant now = Instant.now();
        Check check = checkWithInterval(60, now.minus(61, ChronoUnit.SECONDS));

        assertThat(CheckScheduler.isDue(check, now)).isTrue();
    }

    @Test
    void tickPingsOnlyDueChecks() {
        ReflectionTestUtils.setField(scheduler, "schedulerEnabled", true);
        Instant now = Instant.now();
        Check due = checkWithInterval(60, now.minus(120, ChronoUnit.SECONDS));
        Check notDue = checkWithInterval(60, now.minus(5, ChronoUnit.SECONDS));
        when(checkRepository.findByEnabledTrue()).thenReturn(List.of(due, notDue));
        when(pingService.ping("https://google.com"))
                .thenReturn(new PingResponse("https://google.com", CheckStatus.UP, 10L, 200, null));

        scheduler.tick();

        verify(checkRunService).recordResult(any(Check.class), any(PingResponse.class));
    }

    @Test
    void tickSkipsEverythingWhenDisabled() {
        ReflectionTestUtils.setField(scheduler, "schedulerEnabled", false);

        scheduler.tick();

        verify(checkRepository, never()).findByEnabledTrue();
    }

    @Test
    void runSingleSwallowsUnexpectedErrors() {
        Check check = checkWithInterval(60, null);
        when(pingService.ping("https://google.com")).thenThrow(new RuntimeException("boom"));

        scheduler.runSingle(check);

        verify(checkRunService, never()).recordResult(any(), any());
    }
}
