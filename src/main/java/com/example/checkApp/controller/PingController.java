package com.example.checkApp.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.checkApp.dto.request.PingRequest;
import com.example.checkApp.dto.response.PingResponse;
import com.example.checkApp.service.PingService;

import jakarta.validation.Valid;

/**
 * «Одиночный пинг»: быстро проверить доступность одного URL, не создавая чек.
 *
 * <p>Этот эндпоинт <b>публичный</b> (разрешён в {@code SecurityConfig}): его задача —
 * дать API-«пингером» и Swagger UI возможность быстро спросить «жив ли сайт?».
 * Он ничего не пишет в БД — только выполняет HTTP-запрос и возвращает результат.</p>
 *
 * <p>Постоянные проверки по расписанию — это уже чеки ({@link CheckController});
 * этот контроллер — разовый «разведывательный» запрос.</p>
 */
@Tag(name = "Ping", description = "One-shot URL availability probe")
@RestController
@RequestMapping("/api")
public class PingController {

    private final PingService pingService;

    public PingController(PingService pingService) {
        this.pingService = pingService;
    }

    /**
     * Пингует указанный URL один раз.
     *
     * <p>Пример тела запроса: {@code {"url": "https://google.com"}}.
     * Ответ: статус UP/DOWN, код ответа (если был), время отклика в мс.</p>
     *
     * <p>{@code @Valid} включает проверку {@link PingRequest} (@URL на поле url) →
     * кривой URL даст 400 Bad Request, а не сетевую попытку соединения.</p>
     */
    @PostMapping("/ping")
    public PingResponse ping(@Valid @RequestBody PingRequest request) {
        return pingService.ping(request.url());
    }
}
