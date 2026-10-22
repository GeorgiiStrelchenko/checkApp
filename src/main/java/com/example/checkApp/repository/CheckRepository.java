package com.example.checkApp.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.checkApp.entity.Check;

/**
 * Репозиторий чеков (JPA/Spring Data).
 *
 * <p>Интерфейс пустой по реализации: Spring Data сам генерирует SQL-имплементацию
 * на старте приложения. Имена методов читаются как запросы — это <b>derived queries</b>:
 * разбор идёт по «существительным и глаголам» метода (см. ниже).</p>
 *
 * <p>{@code JpaRepository<Check, Long>} даёт «из коробки»: {@code save}, {@code findById},
 * {@code findAll}, {@code deleteById}, {@code count} и пагинацию.</p>
 */
public interface CheckRepository extends JpaRepository<Check, Long> {

    /**
     * Derived query: {@code SELECT * FROM checks WHERE enabled = true}.
     * Используется планировщиком — проверяем только включённые чеки.
     */
    List<Check> findByEnabledTrue();

    /**
     * {@code SELECT * FROM checks WHERE user_id = ?} — все чеки пользователя.
     * Ключевой запрос изоляции: так {@code CheckService.findAll} отдаёт только свои чеки.
     */
    List<Check> findByUserId(Long userId);
}
