package com.example.checkApp;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Общий базовый класс тестов, которым нужен настоящий PostgreSQL.
 *
 * <p><b>Идея Testcontainers:</b> вместо H2 (другая БД = «работает в тестах,
 * падает в проде») тесты поднимают настоящий контейнер {@code postgres:17} —
 * те же диалект, миграции Flyway и поведение, что и в проде.</p>
 *
 * <p><b>Как это работает (по шагам):</b></p>
 * <ol>
 *   <li>При загрузке тестового класса выполняется статический инициализатор —
 *       {@link #POSTGRES}: создаётся и стартует ОДИН контейнер на весь JVM
 *       (все тестовые классы делят его — так быстро; контейнер убьёт Testcontainers/Ryuk
 *       после завершения тестов);</li>
 *   <li>Аннотация {@link DynamicPropertySource} на методе — Spring-хук «заполнить
 *       свойства конфигурации динамически»: сюда приходят url/username/password
 *       ЖИВОГО контейнера (порт, например 32769, заранее неизвестен);</li>
 *   <li>эти свойства перекрывают значения из application.yml, и все
 *       {@code @SpringBootTest}/{@code @DataJpaTest} подключаются к контейнеру;</li>
 *   <li>Flyway при старте контекста накатывает миграции на пустую базу.</li>
 * </ol>
 *
 * <p>{@code @DynamicPropertySource} ищется и в СУПЕРКЛАССЕ (Spring обходит
 * иерархию через MethodIntrospector) — поэтому метод лежит здесь, а не продублирован
 * в каждом тесте.</p>
 *
 * <p><b>Почему наследники не создают свой контейнер:</b> статическое поле
 * инициализируется один раз на JVM — все {@code extends PostgresTestSupport}
 * пользуются одним и тем же экземпляром.</p>
 *
 * <p>Требования: запущенный Docker (иначе {@code container.start()} упадёт с ошибкой).</p>
 */
public abstract class PostgresTestSupport {

    /** Единственный контейнер на всю JVM; инициализация = создание + start(). */
    private static final PostgreSQLContainer POSTGRES = startPostgres();

    /** Фабрика: берём образ postgres:17 и поднимаем его. */
    private static PostgreSQLContainer startPostgres() {
        PostgreSQLContainer container = new PostgreSQLContainer("postgres:17");
        container.start(); // блокирует поток до готовности БД (healthcheck внутри)
        return container;
    }

    /**
     * Подставляет свойства подключённого контейнера в Spring-конфигурацию.
     *
     * <p>Ключи — обычные spring.datasource.*; значения ленивые ({@code ::getJdbcUrl}):
     * Spring вызовет их при чтении конфигурации, когда контейнер уже гарантированно работает.</p>
     */
    @DynamicPropertySource
    static void registerDatasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }
}
