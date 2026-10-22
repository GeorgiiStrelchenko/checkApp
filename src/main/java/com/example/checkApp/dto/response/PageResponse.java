package com.example.checkApp.dto.response;

import java.util.List;

/**
 * Универсальный DTO страницы для любого пагинированного списка (сейчас — история результатов).
 *
 * <p>Почему свой DTO, а не отдавать Spring-овский {@code Page}: Page тащит в JSON
 * служебные поля (sort, pageable, last, first...) — много шума. Здесь — ровно то,
 * что нужно клиенту для отрисовки пагинации: содержимое + счётчики.</p>
 *
 * <p>Дженерик {@code <T>}: одна обёртка работает и для {@code CheckResultResponse},
 * и для любого другого элемента, если появятся другие списки.</p>
 *
 * @param <T>             тип элементов страницы
 * @param content         элементы текущей страницы
 * @param page            номер текущей страницы, с 0
 * @param size            размер страницы
 * @param totalElements   всего элементов по всем страницам
 * @param totalPages      всего страниц (ceil(totalElements / size))
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
