package com.example.checkApp.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.checkApp.entity.CheckResult;

/**
 * Репозиторий истории проверок (одна строка = один пинг).
 *
 * <p>Как и в {@link CheckRepository}, запросы собираются Spring Data из имён методов.
 * Таблица самая «ростовая» в проекте: каждая проверка пишет строку,
 * поэтому здесь важна сортировка и пагинация.</p>
 */
public interface CheckResultRepository extends JpaRepository<CheckResult, Long> {

    /**
     * {@code ... LIMIT 20 ORDER BY checked_at DESC} — последние 20 результатов чека.
     * Используется в {@link com.example.checkApp.service.CheckRunService} (тесты)
     * и там, где нужен «хвост» истории без пагинации.
     */
    List<CheckResult> findTop20ByCheckIdOrderByCheckedAtDesc(Long checkId);

    /**
     * История с пагинацией: {@link Pageable} несёт номер страницы, размер и сортировку
     * (их задаёт сервис: page/size + DESC по checkedAt). Возвращает {@link Page} —
     * объект с содержимым <b>и</b> метаданными (всего записей, всего страниц).
     */
    Page<CheckResult> findByCheckId(Long checkId, Pageable pageable);

    /**
     * Результаты чека, сделанные не раньше {@code from} — выборка для uptime-статистики
     * за окно в часах (границу окна считает {@link com.example.checkApp.service.CheckHistoryService}).
     */
    List<CheckResult> findByCheckIdAndCheckedAtGreaterThanEqualOrderByCheckedAtAsc(Long checkId, Instant from);
}
