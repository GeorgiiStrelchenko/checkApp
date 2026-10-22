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

/**
 * Фоновый «сердцебиение» мониторинга: периодически обходит включённые чеки
 * и проверяет те, у которых подошёл интервал.
 *
 * <p><b>Как устроено расписание</b> ({@code @Scheduled} требует {@code @EnableScheduling}
 * — она стоит на главном классе {@code CheckAppApplication}):</p>
 * <ul>
 *   <li>{@code fixedDelayString = "${app.scheduler.tick-ms:10000}"} — «тик» раз в N миллисекунд
 *       (по умолчанию 10 с). Используется <b>fixedDelay</b>, а не fixedRate: следующий тик
 *       стартует через N мс <i>после завершения</i> предыдущего — долгие пинги не накапливаются
 *       и не выполняются параллельно;</li>
 *   <li>значение тика читается из конфига — в тестах планировщик выключается
 *       ({@code app.scheduler.enabled: false}), иначе фоновые пинги мешали бы тестам.</li>
 * </ul>
 *
 * <p><b>Почему не @Scheduled на каждый чек:</b> у каждого чека свой интервал (например, 60 с
 * или 3600 с), а расписаний в Spring — ограниченный набор. Поэтому один тик «тик»
 * сам решает, каким чекам пора (см. {@link #isDue}) — это проще и масштабируется
 * на любое количество чеков.</p>
 *
 * <p><b>Отказоустойчивость:</b> ошибка одного чека не роняет весь тик
 * (try-catch в {@link #runSingle}), планировщик работает в одном потоке по умолчанию.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CheckScheduler {

    private final CheckRepository checkRepository;
    private final PingService pingService;
    private final CheckRunService checkRunService;

    /**
     * Мастер-переключатель: выключен = тик сразу выходит, ничего не пингует.
     * В проде — true (default), в тестах — false (application.yml в test/resources).
     */
    @Value("${app.scheduler.enabled:true}")
    private boolean schedulerEnabled;

    /**
     * Один тик планировщика.
     *
     * <p>Алгоритм: взять все включённые чеки и для каждого проверить,
     * прошло ли достаточно времени с последней проверки. Пинги идут последовательно —
     * для пет-проекта это нормально (нет нужды в пуле потоков).</p>
     */
    @Scheduled(fixedDelayString = "${app.scheduler.tick-ms:10000}")
    public void tick() {
        if (!schedulerEnabled) {
            return;
        }
        Instant now = Instant.now();
        // findByEnabledTrue() — включённые чеки; выключенные не проверяются вовсе
        for (Check check : checkRepository.findByEnabledTrue()) {
            if (isDue(check, now)) {
                runSingle(check);
            }
        }
    }

    /**
     * Проверяет один чек: HTTP-пинг → сохранение результата (история + алерт при смене статуса).
     *
     * <p>package-private (без public) — его напрямую дергают тесты
     * ({@code CheckSchedulerTest}), минуя расписание.</p>
     *
     * <p>Любое исключение перехватываем: упавший пинг одного чека (например, кривой URL)
     * не должен останавливать проверку остальных в этом тике.</p>
     */
    void runSingle(Check check) {
        try {
            PingResponse ping = pingService.ping(check.getUrl());
            checkRunService.recordResult(check, ping);
        } catch (Exception e) {
            log.error("Unexpected error while checking '{}' ({}): {}", check.getName(), check.getUrl(), e.getMessage());
        }
    }

    /**
     * Пора ли проверять чек сейчас.
     *
     * <p>Логика: интервал прошёл, если {@code lastCheckedAt + interval <= now}:</p>
     * <ul>
     *   <li>чек ещё ни разу не проверялся ({@code last == null}) → да, сразу;</li>
     *   <li>иначе сравниваем момент следующей обязательной проверки с текущим временем:
     *       если он не в будущем ({@code !...isAfter(now)}) → пора.</li>
     * </ul>
     *
     * <p>Почему не «ровно каждые N секунд от старта»: тик общий для всех чеков,
     * каждый чек живёт по своему «графику» от собственной последней проверки.</p>
     *
     * @param check чек
     * @param now   текущее время (передаётся параметром, чтобы тесты могли подставить своё)
     */
    static boolean isDue(Check check, Instant now) {
        Instant last = check.getLastCheckedAt();
        if (last == null) {
            return true;
        }
        // last + intervalSeconds > now → ещё рано проверять
        return !last.plusSeconds(check.getCheckIntervalSeconds()).isAfter(now);
    }
}
