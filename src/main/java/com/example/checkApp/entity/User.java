package com.example.checkApp.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA-сущность «пользователь» — владелец чеков, учётная запись для входа.
 *
 * <p>Таблица {@code users} (в PostgreSQL {@code user} — зарезервированное слово,
 * поэтому имя именно такое; встречается оно и в миграции V2).</p>
 *
 * <p>Важно: в этой сущности <b>нет</b> поля «сырой пароль». То, что лежит в
 * {@code password} — BCrypt-хэш, записанный в {@code UserService.register}.
 * Так утекшая база пользователей не выдаёт пароли в открытом виде.</p>
 *
 * <p>Связь с чеками обратная (у чека поле user) — руками здесь список чеков не держим,
 * чтобы не тянуть лишние данные и не плодить дублирование (см. LAZY у Check.user).</p>
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Логин; {@code unique = true} — страховка от дубликатов на уровне схемы
     * (помимо проверки existsByUsername в сервисе: гонку двух одновременных
     * регистраций ловит только ограничение БД).
     */
    @Column(nullable = false, unique = true)
    private String username;

    /** BCrypt-хэш пароля (не сам пароль!). */
    @Column(nullable = false)
    private String password;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** Хук JPA: ставит дату регистрации перед первым INSERT. */
    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
