package com.example.checkApp.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.checkApp.dto.response.CheckResultResponse;
import com.example.checkApp.dto.response.CheckStatsResponse;
import com.example.checkApp.dto.response.PageResponse;
import com.example.checkApp.entity.Check;
import com.example.checkApp.entity.CheckResult;
import com.example.checkApp.exception.NotFoundException;
import com.example.checkApp.model.CheckStatus;
import com.example.checkApp.repository.CheckRepository;
import com.example.checkApp.repository.CheckResultRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CheckHistoryService {

    private final CheckRepository checkRepository;
    private final CheckResultRepository checkResultRepository;

    public PageResponse<CheckResultResponse> getResults(Long checkId, int page, int size) {
        requireCheck(checkId);
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "checkedAt"));
        Page<CheckResult> resultPage = checkResultRepository.findByCheckId(checkId, pageable);
        return new PageResponse<>(
                resultPage.getContent().stream().map(CheckHistoryService::toResponse).toList(),
                resultPage.getNumber(),
                resultPage.getSize(),
                resultPage.getTotalElements(),
                resultPage.getTotalPages()
        );
    }

    public CheckStatsResponse getStats(Long checkId, int hours) {
        requireCheck(checkId);
        Instant from = Instant.now().minus(hours, ChronoUnit.HOURS);
        List<CheckResult> results =
                checkResultRepository.findByCheckIdAndCheckedAtGreaterThanEqualOrderByCheckedAtAsc(checkId, from);

        long total = results.size();
        long upCount = results.stream().filter(r -> r.getStatus() == CheckStatus.UP).count();
        long downCount = total - upCount;
        double uptimePercent = total == 0 ? 100.0 : Math.round(upCount * 10000.0 / total) / 100.0;

        List<Long> responseTimes = results.stream()
                .map(CheckResult::getResponseTimeMs)
                .filter(java.util.Objects::nonNull)
                .toList();
        Long avg = responseTimes.isEmpty() ? null
                : Math.round(responseTimes.stream().mapToLong(Long::longValue).average().orElse(0));
        Long max = responseTimes.isEmpty() ? null
                : responseTimes.stream().mapToLong(Long::longValue).max().orElse(0);

        return new CheckStatsResponse(checkId, hours, total, upCount, downCount, uptimePercent, avg, max);
    }

    private void requireCheck(Long checkId) {
        Check check = checkRepository.findById(checkId)
                .orElseThrow(() -> new NotFoundException("Check", checkId));
    }

    private static CheckResultResponse toResponse(CheckResult result) {
        return new CheckResultResponse(
                result.getId(),
                result.getStatus(),
                result.getHttpCode(),
                result.getResponseTimeMs(),
                result.getErrorMessage(),
                result.getCheckedAt()
        );
    }
}
