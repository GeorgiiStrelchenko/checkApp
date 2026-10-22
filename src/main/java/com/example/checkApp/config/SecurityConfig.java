package com.example.checkApp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

/**
 * Конфигурация Spring Security: кто и куда может ходить, как логинимся/логаутимся, чем шифруем пароли.
 *
 * <p>Модель безопасности в проекте:</p>
 * <ul>
 *   <li><b>Web-страницы</b> (Thymeleaf: дашборд, страница чека) — только для залогиненных;
 *       неавторизованный браузерный запрос получает редирект 302 на {@code /login}.</li>
 *   <li><b>REST API</b> ({@code /api/**}) — только для залогиненных, но без редиректа:
 *       неавторизованный запрос получает <b>401 Unauthorized</b> (JSON-клиент не должен
 *       получать HTML страницу логина).</li>
 *   <li><b>Публичные</b> — логин, регистрация, документация Swagger.</li>
 * </ul>
 *
 * <p>Аутентификация — form login: логин POST {@code /login} (Spring Security сам обрабатывает),
 * сессия хранится в cookie {@code JSESSIONID}. Пароли хранятся в BCrypt-хэше (см. {@link #passwordEncoder()}).</p>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Главный фильтр безопасности Spring Security: описывает цепочку правил HTTP-запросов.
     *
     * <p>Порядок важен: Spring идёт сверху вниз, но здесь правила не пересекаются.</p>
     *
     * <p>Про {@code 401 vs 302}: Security 7 регистрирует точку входа form login'а
     * (редирект на {@code /login}) как <i>дефолтную</i> для «браузерных» запросов
     * (с заголовком {@code Accept: text/html}). Наша же 401-точка входа привязана к паттерну
     * {@code /api/**} через {@code defaultAuthenticationEntryPointFor} и ловит остальное.
     * Итог поведения:</p>
     * <ul>
     *   <li>браузер (Accept: text/html) заходит на {@code /} → 302 на {@code /login};</li>
     *   <li>curl/клиент без Accept на {@code /api/checks} → 401;</li>
     *   <li>любой запрос к {@code /api/**} без логина → 401 (паттерн приоритетнее).</li>
     * </ul>
     *
     * @param http билдер конфигурации Security (аналог {@code http.authorizeHttpRequests(...)} из DSL)
     * @return построенная цепочка фильтров, Spring подключит её к Servlet-контейнеру
     */
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 1) Кому что разрешено
                .authorizeHttpRequests(auth -> auth
                        // публичные страницы и эндпоинты: логин, регистрация (форма и REST),
                        // одиночный пинг, страница ошибок и документация OpenAPI/Swagger
                        .requestMatchers("/login", "/register", "/api/register", "/api/ping", "/error",
                                "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
                        .permitAll()
                        // всё остальное (дашборд, страницы чеков, остальной API) — требует аутентификации
                        .anyRequest().authenticated())
                // 2) Form login: страница логина своя (Thymeleaf), после успешного логина — на главную.
                //    Spring Security сам принимает POST /login и валидирует пароль через UserDetailsService.
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/", true)
                        .permitAll())
                // 3) Выход: POST /logout (CSRF обязателен) → редирект на страницу логина с флажком logout
                .logout(logout -> logout
                        .logoutSuccessUrl("/login?logout")
                        .permitAll())
                // 4) CSRF: включён для форм (HTML), но отключён для REST /api/**
                //    — API логинится не через формы, и токен в каждом запросе только мешал бы
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/api/**"))
                // 5) Точки входа для НЕаутентифицированных: для /api/** — чистый 401 вместо редиректа на /login
                .exceptionHandling(exceptions -> exceptions
                        .defaultAuthenticationEntryPointFor(
                                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                                PathPatternRequestMatcher.withDefaults().matcher("/api/**")));
        return http.build();
    }

    /**
     * Стратегия хеширования паролей.
     *
     * <p>BCrypt — адаптивный хэш с солью: одинаковый пароль даёт разные хэши,
     * подобрать пароль по хэшу практически невозможно. В БД пароль хранится как
     * {@code $2a$...} — это хэш, а не «пароль в открытом виде».</p>
     *
     * <p>Используется в двух местах:</p>
     * <ul>
     *   <li>{@code UserService} при регистрации — {@code passwordEncoder.encode(rawPassword)};</li>
     *   <li>Spring Security при логине — {@code passwordEncoder.matches(rawPassword, hash)}.</li>
     * </ul>
     */
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
