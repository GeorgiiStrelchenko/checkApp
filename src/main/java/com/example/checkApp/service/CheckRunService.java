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

/**
 * «Ядро» мониторинга: фиксирует результат пинга в БД и решает, нужен ли алерт.
 *
 * <p>Вызывается из {@link com.example.checkApp.scheduler.CheckScheduler} после каждого
 * HTTP-пинга. За один вызов метод {@link #recordResult} делает три вещи:</p>
 * <ol>
 *   <li>сохраняет новую строку в {@code check_results} (история);</li>
 *   <li>обновляет «последнее состояние» в самом чеке (lastStatus/lastCheckedAt)
 *       — чтобы дашборд показывал текущий статус без запроса истории;</li>
 *   <li>сравнивает старый и новый статус и при смене шлёт алерт.</li>
 * </ol>
 *
 * <p>{@code @Slf4j} (Lombok) добавляет поле {@code log} — пишем каждую проверку в лог,
 * чтобы по логам можно было смотреть «жив ли был сайт в 3 ночи».</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CheckRunService {

    /**
     * Максимальная длина текста ошибки в БД: колонка VARCHAR(2048),
     * а сообщения сетевых библиотек бывают и длиннее — обрезаем, чтобы INSERT не упал.
     */
    private static final int MAX_ERROR_MESSAGE_LENGTH = 2048;

    private final CheckResultRepository checkResultRepository;
    private final CheckRepository checkRepository;
    private final AlertNotifier alertNotifier;

    /**
     * Записывает результат одного пинга и, при необходимости, оповещает о смене статуса.
     *
     * <p>Вся операция в одной транзакции: история и обновление чека должны либо сохраниться
     * вместе, либо откатиться вместе.</p>
     *
     * @param check чек, который проверяли (уже загружен планировщиком)
     * @param ping  результат HTTP-пинга (статус UP/DOWN, код, время отклика, ошибка)
     * @return сохранённая запись истории
     */
    @Transactional
    public CheckResult recordResult(Check check, PingResponse ping) {
        // Статус ДО проверки — он понадобится, чтобы понять, произошла ли смена статуса
        CheckStatus previousStatus = check.getLastStatus();

        // 1) история: одна строка на каждую проверку
        CheckResult result = new CheckResult();
        result.setCheck(check);
        result.setStatus(ping.status());
        result.setHttpCode(ping.httpCode());
        result.setResponseTimeMs(ping.responseTimeMs());
        result.setErrorMessage(truncate(ping.errorMessage()));
        checkResultRepository.save(result);

        // 2) «последнее состояние» чека — для дашборда и для сравнения статусов при следующем пинге
        check.setLastStatus(ping.status());
        check.setLastCheckedAt(Instant.now());
        checkRepository.save(check);

        log.info("Check '{}' ({}) -> {} ({} ms, code {})",
                check.getName(), check.getUrl(), ping.status(), ping.responseTimeMs(), ping.httpCode());

        // 3) алерт — только на СМЕНУ статуса, не на каждую проверку
        if (isStatusChange(previousStatus, ping.status())) {
            alertNotifier.notifyStatusChange(check, previousStatus, ping.status());
        }
        return result;
    }

    /**
     * Нужен ли алерт при переходе {@code previousStatus → currentStatus}.
     *
     * <p>Правила:</p>
     * <ul>
     *   <li>чек проверяется впервые ({@code previousStatus == null}): алерт <b>только</b>
     *       если сайт сразу DOWN — «чек создан, а он уже лежит» важно знать сразу;</li>
     *   <li>иначе — алерт при любой смене (UP→DOWN «сайт упал!» и DOWN→UP «восстановился»).</li>
     *   <li>статус не изменился → алерта нет (иначе спам каждые N секунд).</li>
     * </ul>
     */
    private static boolean isStatusChange(CheckStatus previousStatus, CheckStatus currentStatus) {
        if (previousStatus == null) {
            return currentStatus == CheckStatus.DOWN;
        }
        return previousStatus != currentStatus;
    }

    /** Обрезка текста ошибки до {@value #MAX_ERROR_MESSAGE_LENGTH} символов (защита колонки от переполнения). */
    private static String truncate(String value) {
        if (value == null || value.length() <= MAX_ERROR_MESSAGE_LENGTH) {
            return value;
        }
        return value.substring(0, MAX_ERROR_MESSAGE_LENGTH);
    }
}
