package com.example.checkApp.entity;

import java.time.Instant;

import com.example.checkApp.model.CheckStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA-сущность «чек» — проверяемый сайт/сервис пользователя.
 *
 * <p>Маппится на таблицу {@code checks} (структуру задаёт Flyway-миграция V1;
 * {@code ddl-auto: validate} — Hibernate схему <b>проверяет</b> на соответствие,
 * но не меняет: источник правды по БД — миграции, а не код).</p>
 *
 * <p>Поля делятся на три группы:</p>
 * <ul>
 *   <li><b>описание</b> — name, url, checkIntervalSeconds, enabled (задаёт пользователь);</li>
 *   <li><b>владелец</b> — user (связь «много чеков → один пользователь»);</li>
 *   <li><b>текущее состояние</b> — lastStatus, lastCheckedAt (обновляет планировщик
 *       при каждой проверке, чтобы дашборд читал статус без запроса истории).</li>
 * </ul>
 *
 * <p>Lombok: {@code @Getter}/{@code @Setter} генерируют геттеры/сеттеры,
 * {@code @NoArgsConstructor} — пустой конструктор, который требует JPA/Hibernate
 * (сущность создаётся через рефлексию, без конструктора с параметрами).</p>
 */
@Entity
@Table(name = "checks")
@Getter
@Setter
@NoArgsConstructor
public class Check {

    /** Первичный ключ, генерируется БД (IDENTITY = AUTO_INCREMENT/sequence). */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Человекочитаемое имя чека («Google», «Мой бэкенд»); обязательное. */
    @Column(nullable = false)
    private String name;

    /** Проверяемый URL; длина до 2048 — длинные URL с query-строками не обрезаются. */
    @Column(nullable = false, length = 2048)
    private String url;

    /**
     * Как часто проверять, в секундах (по умолчанию 60).
     * Имя колонки явное: {@code interval} — зарезервированное слово в SQL.
     */
    @Column(name = "check_interval_seconds", nullable = false)
    private int checkIntervalSeconds = 60;

    /**
     * Включён ли чек: выключенный НЕ проверяется планировщиком (но остаётся в БД).
     * default true — «сразу после создания начинаем проверять».
     */
    @Column(nullable = false)
    private boolean enabled = true;

    /**
     * Владелец чека. {@code LAZY} — пользователь подгружается по требованию,
     * а не вместе с каждым чеком (в списках имя владельца не нужно — оно и так известно).
     *
     * <p>Удаление чеков владельца каскадное — см. миграцию V2 (ON DELETE CASCADE).</p>
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    /**
     * Последний известный статус (UP/DOWN/null = «ещё не проверялся»).
     * Хранится строкой ({@code EnumType.STRING}), а не ordinal'ом: иначе добавление
     * нового значения в enum «поедет» существующие данные в БД.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "last_status")
    private CheckStatus lastStatus;

    /** Момент последней проверки; по нему планировщик считает, когда следующая (см. CheckScheduler.isDue). */
    @Column(name = "last_checked_at")
    private Instant lastCheckedAt;

    /** Момент создания; {@code updatable = false} — после INSERT не меняется. */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * JPA-хук: вызывается автоматически перед первым INSERT.
     * Ставит createdAt, если его не задали руками — удобно и в проде, и в тестах.
     */
    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
