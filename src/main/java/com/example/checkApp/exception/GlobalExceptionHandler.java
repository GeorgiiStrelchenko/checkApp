package com.example.checkApp.exception;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.validation.ConstraintViolationException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

/**
 * Централизованный обработчик ошибок REST API.
 *
 * <p>{@code @RestControllerAdvice} = «перехвати исключение в любом контроллере
 * и верни JSON» — без него Spring отдал бы HTML-страницу ошибки (Whitelabel),
 * а API-клиенту нужен структурированный ответ. Каждый метод ниже — превращение
 * одного вида исключения в пару «код + {@link ApiError}».</p>
 *
 * <p>Покрытые случаи:</p>
 * <table border="1">
 *   <caption>Маппинг исключений на HTTP-статусы</caption>
 *   <tr><th>Исключение</th><th>Статус</th><th>Когда</th></tr>
 *   <tr><td>MethodArgumentNotValidException</td><td>400</td><td>нарушен @Valid на @RequestBody (DTO)</td></tr>
 *   <tr><td>ConstraintViolationException</td><td>400</td><td>нарушен @Min/@Max на параметре запроса (?size=...)</td></tr>
 *   <tr><td>HttpMessageNotReadableException</td><td>400</td><td>тело не парсится как JSON / кривые типы</td></tr>
 *   <tr><td>NotFoundException</td><td>404</td><td>нет ресурса или он чужой</td></tr>
 *   <tr><td>ConflictException</td><td>409</td><td>логин уже занят</td></tr>
 * </table>
 *
 * <p>Необработанные исключения (500) Spring отдаёт как обычно — в пет-проекте
 * отдельный перехват на 500 не понадобился.</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Ошибки валидации тела запроса (@Valid на DTO): собираем поимённо,
     * какое поле и почему не прошло.
     *
     * <p>{@code (first, second) -> first} — дубликаты полей (бывает при нескольких
     * аннотациях) не роняют Collectors.toMap: оставляем первое сообщение.</p>
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, WebRequest request) {
        var fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        fe -> fe.getField(),
                        fe -> fe.getDefaultMessage() == null ? "invalid" : fe.getDefaultMessage(),
                        (first, second) -> first));
        return build(HttpStatus.BAD_REQUEST, "Validation failed", request, fieldErrors);
    }

    /**
     * Тело запроса не читается: невалидный JSON, не тот тип поля,
     * неподдерживаемый Content-Type — отвечаем понятным «Malformed request body».
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadable(HttpMessageNotReadableException ex, WebRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Malformed request body", request, Map.of());
    }

    /** Ресурс не найден или чужой → 404 (текст ошибки уже собран в NotFoundException). */
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(NotFoundException ex, WebRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request, Map.of());
    }

    /** Конфликт (логин занят) → 409. */
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiError> handleConflict(ConflictException ex, WebRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request, Map.of());
    }

    /**
     * Нарушения валидации на <b>параметрах</b> метода контроллера
     * ({@code @Min/@Max/@Pattern} на {@code @RequestParam}) — это отдельный вид
     * исключения, отличный от валидации DTO.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException ex,
            WebRequest request) {
        Map<String, String> fieldErrors = ex.getConstraintViolations().stream()
                .collect(Collectors.toMap(
                        cv -> cv.getPropertyPath().toString(),
                        cv -> cv.getMessage(),
                        (first, second) -> first));
        return build(HttpStatus.BAD_REQUEST, "Validation failed", request, fieldErrors);
    }

    /**
     * Собирает единый ответ {@code ApiError}: статус + сообщение + путь запроса + ошибки полей.
     *
     * <p>Путь достаём из {@link ServletWebRequest}: {@code WebRequest} — абстракция,
     * и не всегда гарантирует Servlet-контекст (например, в WebFlux), поэтому
     * проверяем через {@code instanceof}.</p>
     */
    private ResponseEntity<ApiError> build(HttpStatus status, String message, WebRequest request,
            Map<String, String> fieldErrors) {
        String path = request instanceof ServletWebRequest sw
                ? sw.getRequest().getRequestURI()
                : "";
        ApiError body = new ApiError(Instant.now(), status.value(), status.getReasonPhrase(), message, path,
                fieldErrors);
        return ResponseEntity.status(status).body(body);
    }
}
