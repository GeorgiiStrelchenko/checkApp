package com.example.checkApp.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.checkApp.dto.request.RegisterRequest;
import com.example.checkApp.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * REST-регистрация пользователя (API-вариант; у веб-формы свой метод в {@link UiController}).
 *
 * <p>Публичный эндпоинт — вход в систему для API-клиентов:</p>
 * <ol>
 *   <li>{@code POST /api/register} создаёт пользователя;</li>
 *   <li>потом клиент логинится через {@code POST /login} (обрабатывает Spring Security)
 *       и получает cookie {@code JSESSIONID} для последующих запросов.</li>
 * </ol>
 *
 * <p>Тело в ответ не возвращаем: 201 Created + пустое тело — достаточно заголовка
 * {@code Location} (указываем на ещё несуществующий «ресурс профиля» — сознательно
 * не стали добавлять лишний эндпоинт, профиля в проекте нет).</p>
 */
@Tag(name = "Auth", description = "User registration for API access")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class RegisterController {

    private final UserService userService;

    /**
     * Регистрирует пользователя.
     *
     * <p>Контракт ответов (обрабатывается в {@code GlobalExceptionHandler}):</p>
     * <ul>
     *   <li>201 — пользователь создан;</li>
     *   <li>409 Conflict — имя занято ({@link com.example.checkApp.exception.ConflictException});</li>
     *   <li>400 — невалидное тело (пустой username, короткий пароль).</li>
     * </ul>
     *
     * <p>Пароль в DTO приходит открытым, а в БД уходит как BCrypt-хэш — см. {@code UserService}.</p>
     */
    @PostMapping("/register")
    public ResponseEntity<Void> register(@Valid @RequestBody RegisterRequest request) {
        userService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
