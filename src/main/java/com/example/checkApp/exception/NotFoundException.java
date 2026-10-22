package com.example.checkApp.exception;

/**
 * «Ресурс не найден» — ловится {@code GlobalExceptionHandler} и превращается в HTTP 404.
 *
 * <p>В проекте используется в двух смыслах:</p>
 * <ul>
 *   <li>id действительно нет в БД;</li>
 *   <li>id есть, но чек принадлежит <b>другому</b> пользователю — тогда мы
 *       намеренно отдаём ту же 404 (см. {@code CheckService.getOrThrow}):
 *       так API не выдаёт факт существования чужих ресурсов.</li>
 * </ul>
 *
 * <p>Наследуем от {@link RuntimeException} (unchecked): checked-исключения Spring MVC
 * пришлось бы объявлять в сигнатуре каждого метода контроллера.</p>
 */
public class NotFoundException extends RuntimeException {

    /** Формирует стандартное сообщение вида «Check with id 42 not found». */
    public NotFoundException(String resource, Long id) {
        super(resource + " with id " + id + " not found");
    }
}
