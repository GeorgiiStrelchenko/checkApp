package com.example.checkApp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Точка входа Spring Boot приложения — сервиса мониторинга доступности сайтов (uptime-мониторинг).
 *
 * <p>Что здесь важно:</p>
 * <ul>
 *   <li>{@code @SpringBootApplication} = {@code @Configuration} + {@code @EnableAutoConfiguration}
 *       + {@code @ComponentScan}: Spring сам находит все бины в пакете {@code com.example.checkApp}
 *       и его подпакетах (controller, service, repository, ...) и подключает авто-конфигурацию
 *       (DataSource, JPA, Thymeleaf, Security и т.д.) по наличию зависимостей в pom.xml.</li>
 *   <li>{@code @EnableScheduling} — включает поддержку {@code @Scheduled}: без него методы
 *       с аннотацией {@code @Scheduled} в {@link com.example.checkApp.scheduler.CheckScheduler}
 *       просто не выполнялись бы (расписание игнорируется молча).</li>
 * </ul>
 *
 * <p>Запуск: {@code ./mvnw spring-boot:run} или из IDEA (класс {@code CheckAppApplication}).
 * Конфигурация — в {@code src/main/resources/application.yml}.</p>
 */
@SpringBootApplication
@EnableScheduling
public class CheckAppApplication {

    public static void main(String[] args) {
        // SpringApplication.run() создаёт ApplicationContext (контейнер бинов),
        // запускает веб-сервер (Tomcat) и Spring MVC — после этого приложение доступно на :8080
        SpringApplication.run(CheckAppApplication.class, args);
    }

}
