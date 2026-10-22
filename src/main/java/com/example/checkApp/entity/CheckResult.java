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
 * JPA-сущность «результат проверки» — одна строка истории = один выполненный пинг.
 *
 * <p>Таблица {@code check_results} самая «объёмная»: растёт на строку за каждую проверку
 * каждого чека (при интервале 60 с — 1440 строк в сутки на чек).</p>
 *
 * <p>Связь: много результатов → один чек ({@code @ManyToOne}). При удалении чека
 * его история удаляется каскадом (ON DELETE CASCADE в БД — миграция V1),
 * поэтому «осиротевших» результатов не бывает.</p>
 *
 * <p>Почему результаты — отдельная таблица, а не поля чека: чеку нужен только
 * <i>последний</i> статус (для дашборда), а история нужна для графиков и статистики —
 * это разные объёмы данных и разные запросы.</p>
 */
@Entity
@Table(name = "check_results")
@Getter
@Setter
@NoArgsConstructor
public class CheckResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * К какой проверке относится результат. {@code optional = false} — результат
     * без чека невозможен; {@code LAZY} — при чтении истории сам чек не тянется
     * (его данные в DTO не нужны).
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "check_id", nullable = false)
    private Check check;

    /** UP/DOWN — как «прочитал» проверку мониторинг; строкой (см. комментарий к Check.lastStatus). */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CheckStatus status;

    /** HTTP-код ответа сервера; {@code null}, если соединение не удалось (сайт недоступен). */
    @Column(name = "http_code")
    private Integer httpCode;

    /** Время отклика в мс; {@code null}, если замерить не удалось (например, DNS-ошибка). */
    @Column(name = "response_time_ms")
    private Long responseTimeMs;

    /** Текст ошибки соединения; ограничен 2048 (см. CheckRunService.truncate). */
    @Column(name = "error_message", length = 2048)
    private String errorMessage;

    /** Момент проверки — по нему сортируется история и считается uptime-окно. */
    @Column(name = "checked_at", nullable = false, updatable = false)
    private Instant checkedAt;

    /** Хук JPA: проставляем время проверки перед INSERT, если его не задали. */
    @PrePersist
    void onCreate() {
        if (checkedAt == null) {
            checkedAt = Instant.now();
        }
    }
}
