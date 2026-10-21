# checkApp

Сервис мониторинга доступности сайтов: периодически проверяет твои URL, пишет результаты в PostgreSQL,
шлёт алерт в Telegram, если что-то упало, и показывает дашборд с историей и uptime-статистикой.

## Возможности

- CRUD чеков: имя, URL, интервал проверки, вкл/выкл.
- Фоновый планировщик (`@Scheduled`) пингует сайты по HTTP и пишет `UP`/`DOWN` с кодом ответа,
  временем отклика и текстом ошибки.
- Uptime-статистика за период (24 часа по умолчанию): процент доступности, ср./макс. время ответа,
  количество проверок; история результатов с пагинацией.
- Аутентификация: регистрация и логин формами, пароли в BCrypt, владельческий доступ
  (каждый видит только свои чеки, чужой id → 404).
- Telegram-алерт о смене статуса `UP ↔ DOWN` (заглушка с логом `[telegram-stub]`,
  реальная отправка включается переменными `TELEGRAM_BOT_TOKEN` / `TELEGRAM_CHAT_ID`).
- Web-интерфейс на Thymeleaf: логин, регистрация, дашборд, страница чека с графиком статуса.
- REST API + OpenAPI 3.1 со Swagger UI.

## Стек

| Слой        | Технологии |
|-------------|------------|
| Ядро        | Java 21, Spring Boot 4.1.1 (Spring Framework 7) |
| Web         | Spring MVC, Thymeleaf, springdoc-openapi 3.1.1 |
| Безопасность | Spring Security 7 (form login, BCrypt) |
| Данные      | Spring Data JPA (Hibernate), PostgreSQL 17, Flyway |
| Тесты       | JUnit 5, Spring Test/MockMvc, Mockito, AssertJ, Testcontainers 2.0.5 |
| Сборка/деплой | Maven Wrapper 3.10, multi-stage Dockerfile, Docker Compose, GitHub Actions |

## Быстрый старт

### Docker Compose (всё сразу)

```bash
docker compose up --build
# приложение: http://localhost:8080
# Swagger UI:  http://localhost:8080/swagger-ui/index.html
```

Сервисы: `db` (PostgreSQL 17 на порту **5433**, чтобы не конфликтовать с локальным Postgres)
и `app` (собирается из `Dockerfile`, работает от непривилегированного пользователя).

### Локальная разработка

```bash
docker compose up -d db      # только база на localhost:5433
./mvnw spring-boot:run       # приложение на http://localhost:8080
./mvnw test                  # 92 теста, поднимают свой Postgres через Testcontainers
```

Приложение при старте сам накатывает миграции Flyway (`V1__init.sql`, `V2__users_and_ownership.sql`),
`spring.jpa.hibernate.ddl-auto=validate` — схему проверяет, но не меняет.

## API

| Метод   | Путь                          | Описание | Доступ |
|---------|-------------------------------|----------|--------|
| `POST`  | `/api/register`               | Регистрация `{username, password}` → 201 | публичный |
| `POST`  | `/api/ping`                   | Одиночная проверка URL → статус/код/время | публичный |
| `POST`  | `/api/checks`                 | Создать чек → 201 | авторизованный |
| `GET`   | `/api/checks`                 | Список своих чеков | авторизованный |
| `GET`   | `/api/checks/{id}`            | Чек по id (404, если чужой) | авторизованный |
| `PUT`   | `/api/checks/{id}`            | Обновить чек | авторизованный |
| `DELETE`| `/api/checks/{id}`            | Удалить чек | авторизованный |
| `GET`   | `/api/checks/{id}/results`    | История результатов, `?page&size` | авторизованный |
| `GET`   | `/api/checks/{id}/stats`      | Uptime-статистика, `?hours=24` | авторизованный |

Неаутентифицированный запрос к `/api/**` → **401**, браузерный запрос без логина (с заголовком
`Accept: text/html`) → редирект **302** на `/login`.

Страницы UI: `/login`, `/register`, `/` (дашборд), `/checks/{id}` (детали + история).

## Конфигурация

| Переменная окружения | Назначение | По умолчанию |
|----------------------|------------|--------------|
| `SPRING_DATASOURCE_URL` | JDBC URL PostgreSQL | `jdbc:postgresql://localhost:5433/checkapp` |
| `SPRING_DATASOURCE_USERNAME` | пользователь БД | `checkapp` |
| `SPRING_DATASOURCE_PASSWORD` | пароль БД | `checkapp` |
| `TELEGRAM_BOT_TOKEN` | токен Telegram-бота | пусто (режим заглушки) |
| `TELEGRAM_CHAT_ID` | id чата для алертов | пусто |
| `APP_SCHEDULER_ENABLED` | вкл/выкл фоновых проверок | `true` (в тестах `false`) |

## Тесты

92 теста: unit-тесты сервисов/клиента/планировщика, `@WebMvcTest` для контроллеров,
`@DataJpaTest` для репозиториев, интеграционные тесты (регистрация → логин → CRUD через API и UI,
безопасность, OpenAPI).

Интеграционные тесты ходят в настоящий PostgreSQL 17, который поднимает **Testcontainers**
(базовый класс `PostgresTestSupport`, свой контейнер на весь JVM, конфигурация через
`@DynamicPropertySource`). H2 в проекте не используется, `spring.test.database.replace=none` —
Spring не подменяет источник данных встроенной БД. Тесты детерминированы: планировщик выключен,
`@DataJpaTest` откатывают транзакции, интеграционные тесты чистят таблицы до/после.

```bash
./mvnw test   # нужен запущенный Docker
```

## Структура проекта

```
src/main/java/com/example/checkApp/
├── controller/   REST (Check, Ping, Register) + UI (Thymeleaf)
├── service/      бизнес-логика: чеки, история, пинги, алерты, пользователи
├── scheduler/    фоновые проверки (@Scheduled)
├── client/       HTTP-клиент для пинга URL
├── repository/   Spring Data JPA
├── entity/       JPA-сущности (Check, CheckResult, User)
├── dto/          запросы/ответы API
├── config/       SecurityConfig, OpenApiConfig
└── exception/    обработчики ошибок → JSON
src/main/resources/db/migration/   Flyway-миграции
```

## CI

GitHub Actions (`.github/workflows/ci.yml`): сборка и `./mvnw -B verify` на Java 21.
На ubuntu-latest Docker доступен из коробки, поэтому Testcontainers поднимает свой PostgreSQL 17
и идут те же 92 теста.
