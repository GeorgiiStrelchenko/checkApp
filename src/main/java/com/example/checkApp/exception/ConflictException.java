package com.example.checkApp.exception;

/**
 * «Конфликт, ресурс уже существует» — превращается в HTTP 409 Conflict.
 *
 * <p>В проекте бросается один раз: при регистрации на уже занятый логин
 * (см. {@code UserService.register}). Клиент понимает по 409, что нужно
 * взять другое имя, а не повторять запрос.</p>
 */
public class ConflictException extends RuntimeException {

    /** Сообщение вида «User 'alice' already exists». */
    public ConflictException(String resource, String name) {
        super(resource + " '" + name + "' already exists");
    }
}
