package com.example.checkApp;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * «Контекст поднялся?» — минимальный, но важный smoke-тест.
 *
 * <p>{@code @SpringBootTest} без аргументов стартует ВСЁ приложение: авто-конфигурацию,
 * DataSource, Flyway, Security, веб-сервер. Если бины сломаны, циклические ссылки
 * или миграции — упадёт уже здесь. Пустое тело теста — норма: сам факт успешного
 * старта контекста и есть проверка.</p>
 *
 * <p>Для теста нужен Docker: сначала выполняется статический блок
 * {@link PostgresTestSupport} — поднимается контейнер PostgreSQL, и только потом
 * стартует Spring.</p>
 */
@SpringBootTest
class CheckAppApplicationTests extends PostgresTestSupport {

	@Test
	void contextLoads() {
	}

}
