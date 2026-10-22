package com.example.checkApp.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.checkApp.entity.User;

/**
 * Репозиторий пользователей.
 *
 * <p>Два метода критичны для безопасности/консистентности:</p>
 * <ul>
 *   <li>{@code findByUsername} — используется и при регистрации, и в {@code UserDetailsService}
 *       (логин), и в {@code CurrentUserService} (кто сейчас залогинен);</li>
 *   <li>{@code existsByUsername} — быстрая проверка «имя занято?» перед регистрацией
 *       (отдаём 409 Conflict, не создавая дубликат).</li>
 * </ul>
 *
 * <p>Дополнительная страховка от гонки двух одновременных регистраций —
 * уникальный индекс на колонку username в миграции V2 (ошибка БД там уже ловится
 * на уровне обработчика исключений).</p>
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /** Найти пользователя по логину; {@code Optional.empty()} — такого нет. */
    Optional<User> findByUsername(String username);

    /** Существует ли пользователь с таким логином (для проверки дубликата при регистрации). */
    boolean existsByUsername(String username);
}
