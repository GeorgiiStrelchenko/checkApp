package com.example.checkApp.service;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.checkApp.entity.User;
import com.example.checkApp.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * «Кто сейчас залогинен» — единственное место в проекте, где текущий пользователь
 * достаётся из SecurityContext.
 *
 * <p>Идея: в сервисах (CheckService и др.) владелец чека <b>не передаётся параметром</b> —
 * сервис сам спрашивает «кто я?». Так нельзя «перепутать» пользователя, протаскивая его
 * через слои, и нельзя подделать его из HTTP-запроса: SecurityContext заполняет
 * Security-фильтр после проверки пароля/сессии.</p>
 *
 * <p>Метод возвращает именно {@code User} из БД (а не {@code Authentication}):
 * дальше нужен id владельца для запросов к чекам, а id в Authentication нет —
 * там только имя.</p>
 */
@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final UserRepository userRepository;

    /**
     * Находит текущего аутентифицированного пользователя в БД.
     *
     * @throws UsernameNotFoundException если никто не залогинен (нет Authentication)
     *         или пользователя с таким именем уже нет в БД
     */
    @Transactional(readOnly = true)
    public User getUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new UsernameNotFoundException("No authenticated user");
        }
        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new UsernameNotFoundException(authentication.getName()));
    }
}
