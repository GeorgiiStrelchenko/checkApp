package com.example.checkApp.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import java.net.URI;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.example.checkApp.dto.request.CheckRequest;
import com.example.checkApp.dto.response.CheckResponse;
import com.example.checkApp.dto.response.CheckResultResponse;
import com.example.checkApp.dto.response.CheckStatsResponse;
import com.example.checkApp.dto.response.PageResponse;
import com.example.checkApp.service.CheckHistoryService;
import com.example.checkApp.service.CheckService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

/**
 * REST-контроллер чеков (проверяемых сайтов) — основной API проекта.
 *
 * <p>Здесь только «тонкий» слой: принимает HTTP, валидирует входящие данные
 * и делегирует бизнес-логику сервисам ({@link CheckService}, {@link CheckHistoryService}).
 * Контроллер не знает про БД и не пишет логику сам.</p>
 *
 * <p>Конвенции REST, принятые в проекте:</p>
 * <ul>
 *   <li>{@code POST /api/checks} → 201 Created + заголовок {@code Location} на новый ресурс;</li>
 *   <li>{@code GET /api/checks} → 200 и список (только чеки текущего пользователя);</li>
 *   <li>{@code GET /api/checks/{id}} → 200, а если чека нет <i>или он чужой</i> → 404
 *       (сознательно не 403: не раскрываем, что чек существует у другого юзера);</li>
 *   <li>{@code DELETE /api/checks/{id}} → 204 без тела.</li>
 * </ul>
 *
 * <p>Валидация: {@code @Valid} на DTO (Bean Validation) + {@code @Validated} на классе контроллера,
 * чтобы срабатывали {@code @Min/@Max} на параметрах запроса ({@code ?page=...&size=...}).
 * Ошибки валидации и «не найдено» превращаются в JSON-ответы
 * в {@link com.example.checkApp.exception.GlobalExceptionHandler}.</p>
 *
 * <p>Доступ: только аутентифицированные пользователи (см. {@code SecurityConfig}),
 * принадлежность чеков проверяется в сервисе.</p>
 */
@Tag(name = "Checks", description = "CRUD checks, history and statistics")
@RestController
@RequestMapping("/api/checks")
@RequiredArgsConstructor
@Validated
public class CheckController {

    /** CRUD-операции с чеками. */
    private final CheckService checkService;

    /** История результатов и uptime-статистика (выделены в отдельный сервис). */
    private final CheckHistoryService checkHistoryService;

    /**
     * Создать чек.
     *
     * <p>{@code @Valid} на аргументе включает валидацию {@link CheckRequest}
     * (@NotBlank name, @URL url, @Min(5) interval...) — при ошибке вернётся 400.</p>
     *
     * @return 201 Created, тело — созданный чек, {@code Location: /api/checks/{id}}
     */
    @PostMapping
    public ResponseEntity<CheckResponse> create(@Valid @RequestBody CheckRequest request) {
        CheckResponse response = checkService.create(request);
        return ResponseEntity
                .created(URI.create("/api/checks/" + response.id()))
                .body(response);
    }

    /**
     * Список чеков текущего пользователя (владельца берём из SecurityContext — см. CurrentUserService).
     */
    @GetMapping
    public List<CheckResponse> findAll() {
        return checkService.findAll();
    }

    /**
     * Чек по id. Для чужого или несуществующего id — 404 (см. CheckService.findById).
     */
    @GetMapping("/{id}")
    public CheckResponse findById(@PathVariable Long id) {
        return checkService.findById(id);
    }

    /** Полное обновление чека (PUT): приходит весь CheckRequest, отсутствующие поля обнуляются. */
    @PutMapping("/{id}")
    public CheckResponse update(@PathVariable Long id, @Valid @RequestBody CheckRequest request) {
        return checkService.update(id, request);
    }

    /**
     * Удалить чек (вместе с его историей — каскад в БД).
     *
     * <p>{@code @ResponseStatus(NO_CONTENT)} — метод ничего не возвращает,
     * Spring сам проставит 204 (без тела, как и положено по HTTP-спеке).</p>
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        checkService.delete(id);
    }

    /**
     * История результатов чека с пагинацией (новые сверху).
     *
     * @param page  номер страницы, с 0 (по умолчанию 0)
     * @param size  размер страницы, 1..100 (по умолчанию 20); {@code @Min/@Max} работают
     *              благодаря {@code @Validated} на классе
     */
    @GetMapping("/{id}/results")
    public PageResponse<CheckResultResponse> results(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return checkHistoryService.getResults(id, page, size);
    }

    /**
     * Uptime-статистика за период: процент доступности, ср./макс. время ответа, счётчики UP/DOWN.
     *
     * @param hours окно анализа в часах, 1..720 (30 дней), по умолчанию 24
     */
    @GetMapping("/{id}/stats")
    public CheckStatsResponse stats(
            @PathVariable Long id,
            @RequestParam(defaultValue = "24") @Min(1) @Max(720) int hours) {
        return checkHistoryService.getStats(id, hours);
    }
}
