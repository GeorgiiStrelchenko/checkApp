package com.example.checkApp.service;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.checkApp.dto.request.RegisterRequest;
import com.example.checkApp.entity.User;
import com.example.checkApp.exception.ConflictException;
import com.example.checkApp.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * Пользователи: регистрация + мост в Spring Security.
 *
 * <p>Класс реализует {@link UserDetailsService} — это <b>контракт Spring Security</b>:
 * когда приходит POST /login, Security вызывает {@link #loadUserByUsername},
 * получает хэш пароля из БД и сравнивает с тем, что прислал пользователь
 * (через {@link PasswordEncoder#matches}). То есть наш сервис — «источник правды»
 * о пользователях для механизма аутентификации.</p>
 *
 * <p>Пароли: при регистрации сырые пароли нет — только BCrypt-хэш
 * (см. {@code SecurityConfig#passwordEncoder}). Сравнение тоже идёт по хэшу.</p>
 */
@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Регистрация нового пользователя.
     *
     * <p>Проверка {@code existsByUsername} до INSERT — чтобы отдать осмысленный 409 Conflict;
     * гонка двух одновременных регистраций одного имени дополнительно закрыта
     * уникальным ограничением в БД (V2__users_and_ownership.sql, UNIQUE на username).</p>
     *
     * @throws ConflictException если имя уже занято
     */
    @Transactional
    public void register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new ConflictException("User", request.username());
        }
        User user = new User();
        user.setUsername(request.username());
        // encode() — соль + хэш: одинаковые пароли дают разные хэши
        user.setPassword(passwordEncoder.encode(request.password()));
        userRepository.save(user);
    }

    /**
     * Метод контракта {@code UserDetailsService}: найти пользователя для логина.
     *
     * <p>Возвращает готовый {@code UserDetails} для Spring Security: имя, <b>хэш</b> пароля
     * (не сырой!) и роль. Роль одна — {@code ROLE_USER} (уровней доступа в проекте нет,
     * авторизация — только «свой/чужой» на уровне чеков).</p>
     *
     * @throws UsernameNotFoundException если пользователя нет — Security превратит это
     *         в отказ в логине («Bad credentials»)
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(username));
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword()) // это BCrypt-хэш из БД
                .roles("USER")
                .build();
    }
}
