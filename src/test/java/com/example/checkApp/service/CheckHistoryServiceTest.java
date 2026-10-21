package com.example.checkApp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.example.checkApp.dto.response.CheckResultResponse;
import com.example.checkApp.dto.response.CheckStatsResponse;
import com.example.checkApp.dto.response.PageResponse;
import com.example.checkApp.entity.Check;
import com.example.checkApp.entity.CheckResult;
import com.example.checkApp.entity.User;
import com.example.checkApp.exception.NotFoundException;
import com.example.checkApp.model.CheckStatus;
import com.example.checkApp.repository.CheckRepository;
import com.example.checkApp.repository.CheckResultRepository;
import com.example.checkApp.repository.UserRepository;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(CheckHistoryService.class)
class CheckHistoryServiceTest {

    @Autowired
    private CheckHistoryService checkHistoryService;

    @Autowired
    private CheckRepository checkRepository;

    @Autowired
    private CheckResultRepository checkResultRepository;

    @Autowired
    private UserRepository userRepository;

    @MockitoBean
    private CurrentUserService currentUserService;

    private Check check;
    private User alice;

    @BeforeEach
    void setUp() {
        alice = new User();
        alice.setUsername("alice");
        alice.setPassword("encoded");
        alice = userRepository.saveAndFlush(alice);

        when(currentUserService.getUser()).thenReturn(alice);

        check = new Check();
        check.setName("google");
        check.setUrl("https://google.com");
        check.setUser(alice);
        check = checkRepository.saveAndFlush(check);
    }

    private void addResult(CheckStatus status, Long responseTimeMs, Instant checkedAt) {
        CheckResult result = new CheckResult();
        result.setCheck(check);
        result.setStatus(status);
        result.setResponseTimeMs(responseTimeMs);
        result.setHttpCode(status == CheckStatus.UP ? 200 : null);
        result.setCheckedAt(checkedAt);
        checkResultRepository.save(result);
    }

    @Test
    void statsCountsUptimeOverPeriod() {
        Instant now = Instant.now();
        addResult(CheckStatus.UP, 100L, now.minus(1, ChronoUnit.HOURS));
        addResult(CheckStatus.UP, 200L, now.minus(2, ChronoUnit.HOURS));
        addResult(CheckStatus.DOWN, null, now.minus(3, ChronoUnit.HOURS));
        addResult(CheckStatus.UP, 300L, now.minus(25, ChronoUnit.HOURS)); // вне окна 24ч

        CheckStatsResponse stats = checkHistoryService.getStats(check.getId(), 24);

        assertThat(stats.totalChecks()).isEqualTo(3);
        assertThat(stats.upCount()).isEqualTo(2);
        assertThat(stats.downCount()).isEqualTo(1);
        assertThat(stats.uptimePercent()).isEqualTo(66.67);
        assertThat(stats.avgResponseTimeMs()).isEqualTo(150L);
        assertThat(stats.maxResponseTimeMs()).isEqualTo(200L);
    }

    @Test
    void statsReturns100WithNoResults() {
        CheckStatsResponse stats = checkHistoryService.getStats(check.getId(), 24);

        assertThat(stats.totalChecks()).isZero();
        assertThat(stats.uptimePercent()).isEqualTo(100.0);
        assertThat(stats.avgResponseTimeMs()).isNull();
    }

    @Test
    void statsThrows404ForMissingCheck() {
        assertThatThrownBy(() -> checkHistoryService.getStats(404L, 24))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void statsThrows404ForForeignCheck() {
        User bob = new User();
        bob.setUsername("bob");
        bob.setPassword("encoded");
        Long bobId = userRepository.saveAndFlush(bob).getId();

        Check foreign = new Check();
        foreign.setName("other");
        foreign.setUrl("https://other.com");
        foreign.setUser(userRepository.findById(bobId).orElseThrow());
        Long foreignId = checkRepository.saveAndFlush(foreign).getId();

        assertThatThrownBy(() -> checkHistoryService.getStats(foreignId, 24))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void resultsArePaginatedNewestFirst() {
        Instant now = Instant.now();
        for (int i = 0; i < 25; i++) {
            addResult(CheckStatus.UP, 100L, now.minus(i, ChronoUnit.MINUTES));
        }

        PageResponse<CheckResultResponse> page = checkHistoryService.getResults(check.getId(), 0, 10);

        assertThat(page.totalElements()).isEqualTo(25);
        assertThat(page.totalPages()).isEqualTo(3);
        assertThat(page.content()).hasSize(10);
        assertThat(page.content().get(0).checkedAt())
                .isAfterOrEqualTo(page.content().get(1).checkedAt());
    }

    @Test
    void resultsThrow404ForMissingCheck() {
        assertThatThrownBy(() -> checkHistoryService.getResults(404L, 0, 10))
                .isInstanceOf(NotFoundException.class);
    }
}
