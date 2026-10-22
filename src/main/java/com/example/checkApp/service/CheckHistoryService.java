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

/**
 * Чтение истории проверок: пагинированный список результатов и uptime-статистика за период.
 *
 * <p>Сервис только читает (никогда не пишет), поэтому весь класс —
 * {@code @Transactional(readOnly = true)}: меньше нагрузка, Hibernate может отдать
 * запрос в режиме read-only.</p>
 *
 * <p>Как и в {@link CheckService}, каждый публичный метод сначала вызывает
 * {@link #requireCheck}: чужой checkId → 404, т.е. чужую историю не вычитать.</p>
 *
 * <p>Считаем в Java, а не SQL-агрегатами (AVG/SUM): объёмы у пет-проекта маленькие,
 * а код на Java проще объяснить и покрыть тестами (см. {@link CheckHistoryServiceTest}).
 * Для больших данных это стоило бы перенести в {@code SELECT AVG(...) ... GROUP BY}.</p>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CheckHistoryService {

    private final CheckRepository checkRepository;
    private final CheckResultRepository checkResultRepository;
    private final CurrentUserService currentUserService;

    /**
     * История результатов чека: новые сверху, с пагинацией.
     *
     * <p>Spring Data сам собирает {@code LIMIT/OFFSET} из {@link PageRequest},
     * а {@code Sort.by(DESC, "checkedAt")} — порядок сортировки. В ответ уходит
     * «плоский» DTO {@link PageResponse} с метаданными страницы (текущая, размер,
     * всего элементов, всего страниц) — им удобно пользоваться и фронтенду, и тестам.</p>
     *
     * @param checkId id чека (проверяется на принадлежность)
     * @param page    номер страницы, с 0
     * @param size    размер страницы
     */
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

    /**
     * Uptime-статистика за последние {@code hours} часов.
     *
     * <p>Что считаем:</p>
     * <ul>
     *   <li>{@code uptimePercent} — доля проверок со статусом UP, округление до 2 знаков;
     *       если проверок не было — считаем 100% («новый чек, лежать пока нечему»);</li>
     *   <li>{@code avgResponseTimeMs}/{@code maxResponseTimeMs} — среднее и максимум
     *       <b>только по успешным</b> замерам времени: у DOWN-проверок время отклика
     *       может быть null (например, DNS не разрешился), их в расчёт не берём —
     *       поэтому при пустом списке возвращаем null, а не 0.</li>
     * </ul>
     *
     * @param checkId id чека (проверяется на принадлежность)
     * @param hours   окно в часах
     */
    public CheckStatsResponse getStats(Long checkId, int hours) {
        requireCheck(checkId);
        // граница окна: всё, что проверено раньше (now - hours), в статистику не входит
        Instant from = Instant.now().minus(hours, ChronoUnit.HOURS);
        List<CheckResult> results =
                checkResultRepository.findByCheckIdAndCheckedAtGreaterThanEqualOrderByCheckedAtAsc(checkId, from);

        long total = results.size();
        long upCount = results.stream().filter(r -> r.getStatus() == CheckStatus.UP).count();
        long downCount = total - upCount;
        // Math.round(x * 10000) / 100.0 — «округление до двух знаков» без BigDecimal:
        // 2/3 → 66.67, 1/1 → 100.0, 0 проверок → 100.0
        double uptimePercent = total == 0 ? 100.0 : Math.round(upCount * 10000.0 / total) / 100.0;

        // время отклика только там, где оно реально измерено (не null)
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

    /**
     * Проверяет, что чек существует и принадлежит текущему пользователю.
     *
     * <p>Иначе бросает 404 — сознательно не 403, чтобы по ответу API
     * нельзя было перебором id выяснить, есть ли чек у другого юзера.</p>
     */
    private void requireCheck(Long checkId) {
        Check check = checkRepository.findById(checkId)
                .orElseThrow(() -> new NotFoundException("Check", checkId));
        Long currentUserId = currentUserService.getUser().getId();
        if (check.getUser() == null || !currentUserId.equals(check.getUser().getId())) {
            throw new NotFoundException("Check", checkId);
        }
    }

    /** JPA-сущность → DTO ответа. */
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
