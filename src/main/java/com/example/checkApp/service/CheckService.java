package com.example.checkApp.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.checkApp.dto.request.CheckRequest;
import com.example.checkApp.dto.response.CheckResponse;
import com.example.checkApp.entity.Check;
import com.example.checkApp.exception.NotFoundException;
import com.example.checkApp.repository.CheckRepository;

import lombok.RequiredArgsConstructor;

/**
 * Бизнес-логика чеков: создание, чтение, обновление, удаление.
 *
 * <p><b>Главное правило безопасности этого сервиса — изоляция пользователей:</b>
 * каждый оператор проходит через {@link #getOrThrow}, который помимо «существует ли чек»
 * проверяет, принадлежит ли он текущему залогиненному пользователю. Если чек чужой —
 * бросаем {@link NotFoundException} (404), а не Forbidden (403): так API не раскрывает
 * факт существования чека у другого пользователя (валидация через «угадывание» id).</p>
 *
 * <p><b>Транзакции:</b> класс помечен {@code @Transactional} — все публичные методы
 * работают в одной транзакции по умолчанию. Чтение помечено {@code readOnly = true}:
 * Hibernate оптимизирует такие транзакции (нет flush'а, возможно read-only соединение).</p>
 *
 * <p><b>Кто текущий пользователь</b> — не параметр метода, а {@link CurrentUserService},
 * который достаёт его из SecurityContext (см. отдельный класс: так API-контроллер не передаёт
 * юзера руками, и подменить его нельзя).</p>
 */
@Service
@RequiredArgsConstructor
@Transactional
public class CheckService {

    /** Имя ресурса для текстов ошибок «Check not found: id=...». */
    private static final String RESOURCE = "Check";

    private final CheckRepository checkRepository;
    private final CurrentUserService currentUserService;

    /**
     * Создаёт чек и привязывает его к текущему пользователю.
     *
     * <p>Владелец берётся из SecurityContext, а <b>не</b> из запроса:
     * иначе клиент мог бы создать чек «от имени» другого юзера.</p>
     */
    public CheckResponse create(CheckRequest request) {
        Check check = new Check();
        check.setUser(currentUserService.getUser());
        apply(request, check);
        return toResponse(checkRepository.save(check));
    }

    /**
     * Все чеки текущего пользователя (не всех — только свои).
     *
     * <p>Фильтр по владельцу делает сам SQL-запрос
     * {@code findByUserId(...)} — в БД не утекают чужие строки.</p>
     */
    @Transactional(readOnly = true)
    public List<CheckResponse> findAll() {
        return checkRepository.findByUserId(currentUserService.getUser().getId()).stream()
                .map(CheckService::toResponse)
                .toList();
    }

    /** Чек по id с проверкой владельца; чужой/отсутствующий → 404. */
    @Transactional(readOnly = true)
    public CheckResponse findById(Long id) {
        return toResponse(getOrThrow(id));
    }

    /**
     * Полное обновление чека (PUT-семантика): все 4 поля из запроса перезаписывают сущность.
     *
     * <p>Сначала {@code getOrThrow} (находит + проверяет владельца), потом копирование полей
     * и сохранение. Сущность уже отслеживается EntityManager'ом, но явный {@code save}
     * оставлен для ясности и тестопроверяемости.</p>
     */
    public CheckResponse update(Long id, CheckRequest request) {
        Check check = getOrThrow(id);
        apply(request, check);
        return toResponse(checkRepository.save(check));
    }

    /** Удаляет чек; история результатов уходит каскадом (см. миграцию V1 — ON DELETE CASCADE). */
    public void delete(Long id) {
        Check check = getOrThrow(id);
        checkRepository.delete(check);
    }

    /**
     * Достаёт чек по id <b>и проверяет принадлежность текущему пользователю</b>.
     *
     * <p>Если чек не найден <i>или принадлежит другому</i> — один и тот же
     * {@link NotFoundException} с одинаковым текстом: клиент не может отличить
     * «нет такого id» от «есть, но чужой».</p>
     */
    private Check getOrThrow(Long id) {
        Check check = checkRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(RESOURCE, id));
        Long currentUserId = currentUserService.getUser().getId();
        // защита от IDOR (Insecure Direct Object Reference): сверяем владельца
        if (check.getUser() == null || !currentUserId.equals(check.getUser().getId())) {
            throw new NotFoundException(RESOURCE, id);
        }
        return check;
    }

    /** Копирует поля DTO в сущность (общий код для create и update). */
    private static void apply(CheckRequest request, Check check) {
        check.setName(request.name());
        check.setUrl(request.url());
        check.setCheckIntervalSeconds(request.checkIntervalSeconds());
        check.setEnabled(request.enabled());
    }

    /** Преобразует JPA-сущность в DTO ответа: наружу уходят только «безопасные» поля. */
    private static CheckResponse toResponse(Check check) {
        return new CheckResponse(
                check.getId(),
                check.getName(),
                check.getUrl(),
                check.getCheckIntervalSeconds(),
                check.isEnabled(),
                check.getLastStatus(),
                check.getLastCheckedAt(),
                check.getCreatedAt()
        );
    }
}
