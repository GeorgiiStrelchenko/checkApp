package com.example.checkApp.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Глобальная «подмешка» данных во <b>все</b> Thymeleaf-страницы.
 *
 * <p>{@code @ControllerAdvice} расширяет поведение всех {@code @Controller}:
 * метод с {@code @ModelAttribute} выполняется перед каждым рендером страницы,
 * и его результат появляется в модели под указанным именем — здесь {@code username}.</p>
 *
 * <p>Зачем это нужно: в шаблонах ({@code layout.html}) есть шапка с именем текущего
 * пользователя и кнопкой выхода. Без этого совета пришлось бы в каждом методе
 * {@link UiController} вручную доставать пользователя из SecurityContext.</p>
 *
 * <p>Почему не DI-бин из SecurityContext: имя берём из {@link SecurityContextHolder} —
 * это штатный способ Spring Security узнать, кто сейчас аутентифицирован
 * (контекст хранится в ThreadLocal и заполняется фильтрами Security до контроллера).</p>
 */
@ControllerAdvice
public class GlobalModelAdvice {

    /**
     * Возвращает логин текущего пользователя (пустую строку, если никто не залогинен —
     * например, на страницах {@code /login} и {@code /register}).
     *
     * @return значение доступно во всех шаблонах как {@code ${username}}
     */
    @ModelAttribute("username")
    public String username() {
        // getContext() может вернуть пустой контекст на публичных страницах — тогда authentication == null
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null ? "" : authentication.getName();
    }
}
