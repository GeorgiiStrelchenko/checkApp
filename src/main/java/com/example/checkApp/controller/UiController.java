package com.example.checkApp.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.checkApp.dto.request.CheckRequest;
import com.example.checkApp.dto.request.RegisterRequest;
import com.example.checkApp.dto.response.CheckResponse;
import com.example.checkApp.dto.response.CheckStatsResponse;
import com.example.checkApp.exception.ConflictException;
import com.example.checkApp.exception.NotFoundException;
import com.example.checkApp.service.CheckHistoryService;
import com.example.checkApp.service.CheckService;
import com.example.checkApp.service.UserService;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Контроллер веб-интерфейса (Thymeleaf-страницы): логин, регистрация, дашборд, страница чека.
 *
 * <p>Ключевые отличия от REST-контроллера {@link CheckController}:</p>
 * <ul>
 *   <li>{@code @Controller} вместо {@code @RestController}: методы возвращают <b>имя шаблона</b>
 *       или {@code "redirect:/..."}, а не JSON.</li>
 *   <li>Вместо кодов статусов — <b>редиректы</b> (POST/Redirect/GET): после отправки формы
 *       браузер уходит на {@code /}, чтобы F5 не повторял POST-запрос.</li>
 *   <li>Ошибки валидации показываем <i>на той же странице</i> ({@code BindingResult}),
 *       а не отдельной JSON-ошибкой.</li>
 *   <li>{@code @Hidden} — Springdoc исключает этот контроллер из документации OpenAPI
 *       (это UI, а не API).</li>
 * </ul>
 *
 * <p>Все действия защищены Security (пользователь должен быть залогинен), а принадлежность
 * чеков проверяется в {@link CheckService} — чужой id в UI просто вернёт пользователя
 * обратно на дашборд.</p>
 */
@Hidden
@Controller
@RequiredArgsConstructor
public class UiController {

    private final CheckService checkService;
    private final CheckHistoryService checkHistoryService;
    private final UserService userService;

    /**
     * Главная страница — дашборд со списком чеков и их статистикой.
     *
     * <p>Пустой {@code CheckRequest} заранее кладём в модель: шаблон рендерит форму создания
     * через {@code th:object="${checkRequest}}, и Thymeleaf требует, чтобы объект был.
     * {@code containsAttribute} — защита от затирания, если атрибут уже положил
     * обработчик ошибок валидации.</p>
     */
    @GetMapping("/")
    public String dashboard(Model model) {
        if (!model.containsAttribute("checkRequest")) {
            model.addAttribute("checkRequest", new CheckRequest("", "", 60, true));
        }
        populateDashboard(model);
        return "dashboard"; // имя файла: templates/dashboard.html
    }

    /**
     * Создание чека из формы на дашборде.
     *
     * <p>{@code BindingResult} сразу после {@code @ModelAttribute} — «прилипает» к нему
     * и собирает ошибки валидации. Если ошибки есть — рендерим дашборд с ними
     * (без редиректа, чтобы пользователь видел подсветку полей).</p>
     *
     * <p>Успех → flash-сообщение + редирект на главную (flash-атрибут живёт ровно
     * один переход, после редиректа шаблон его «съедает» и показывает).</p>
     */
    @PostMapping("/checks")
    public String createCheck(@Valid @ModelAttribute("checkRequest") CheckRequest request,
            BindingResult binding,
            Model model, RedirectAttributes redirectAttributes) {
        if (binding.hasErrors()) {
            populateDashboard(model);
            return "dashboard";
        }
        checkService.create(request);
        redirectAttributes.addFlashAttribute("message", "Check created");
        return "redirect:/";
    }

    /**
     * Включение/выключение чека (кнопка «toggle» на дашборде).
     *
     * <p>Реализовано через полное обновление (PUT-семантика сервиса): читаем чек,
     * переворачиваем {@code enabled} и сохраняем. {@code NotFoundException} ловим,
     * чтобы не отдавать пользователю 500 — вместо этого flash-сообщение об ошибке.</p>
     */
    @PostMapping("/checks/{id}/toggle")
    public String toggle(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            CheckResponse check = checkService.findById(id);
            checkService.update(id, new CheckRequest(check.name(), check.url(),
                    check.checkIntervalSeconds(), !check.enabled()));
            redirectAttributes.addFlashAttribute("message", "Check updated");
        } catch (NotFoundException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/";
    }

    /** Удаление чека кнопкой; чужой/несуществующий id — тихо возвращаемся на главную с ошибкой. */
    @PostMapping("/checks/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            checkService.delete(id);
            redirectAttributes.addFlashAttribute("message", "Check deleted");
        } catch (NotFoundException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/";
    }

    /**
     * Страница чека: его данные, uptime-статистика за период и последние 20 результатов.
     *
     * @param hours окно статистики из query-параметра (?hours=24), жёстко зажимаем в 1..720,
     *              чтобы пользователь не мог запросить отрицательное или гигантское окно
     * @return шаблон {@code check-detail}; если чека нет или он чужой — редирект на главную
     *         (в UI не показываем 404: для владельца чужого чека это «просто нет такой страницы»)
     */
    @GetMapping("/checks/{id}")
    public String detail(@PathVariable Long id, @RequestParam(defaultValue = "24") int hours, Model model) {
        int normalizedHours = Math.min(Math.max(hours, 1), 720);
        try {
            model.addAttribute("check", checkService.findById(id));
            model.addAttribute("stats", checkHistoryService.getStats(id, normalizedHours));
            model.addAttribute("results", checkHistoryService.getResults(id, 0, 20));
            model.addAttribute("hours", normalizedHours);
            return "check-detail";
        } catch (NotFoundException e) {
            return "redirect:/";
        }
    }

    /**
     * Страница логина (Spring Security сам обрабатывает POST /login с этой формы;
     * здесь только отдаём шаблон — см. {@code SecurityConfig#formLogin}).
     */
    @GetMapping("/login")
    public String login() {
        return "login";
    }

    /** Пустая форма регистрации (объект в модель — как и на дашборде, для {@code th:object}). */
    @GetMapping("/register")
    public String registerForm(Model model) {
        if (!model.containsAttribute("registerRequest")) {
            model.addAttribute("registerRequest", new RegisterRequest("", ""));
        }
        return "register";
    }

    /**
     * Обработка формы регистрации.
     *
     * <p>Два вида ошибок обрабатываются по-разному:</p>
     * <ul>
     *   <li>ошибки валидации (пустой логин, короткий пароль) → остаёмся на странице,
     *       {@code BindingResult} уже содержит их для подсветки полей;</li>
     *   <li>имя занято ({@link ConflictException}) → ловим вручную и кладём сообщение
     *       в модель под ключ {@code error} (шаблон его выводит).</li>
     * </ul>
     *
     * <p>Успех → редирект на {@code /login?registered}: страница логина показывает
     * «Регистрация прошла, войдите».</p>
     */
    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("registerRequest") RegisterRequest request,
            BindingResult binding,
            Model model) {
        if (binding.hasErrors()) {
            return "register";
        }
        try {
            userService.register(request);
        } catch (ConflictException e) {
            model.addAttribute("error", e.getMessage());
            return "register";
        }
        return "redirect:/login?registered";
    }

    /**
     * Наполняет модель данными дашборда: чеки текущего юзера + uptime-статистика каждого за 24 часа.
     *
     * <p>Статистика собирается в карту {@code checkId → stats}: шаблон ищет её по id,
     * чтобы не считать агрегаты в самом шаблоне. {@code NotFoundException} глотаем —
     * чек могли удалить между чтением списка и подсчётом статистики (гонка),
     * и падать из-за этого на весь дашборд не стоит.</p>
     */
    private void populateDashboard(Model model) {
        List<CheckResponse> checks = checkService.findAll();
        Map<Long, CheckStatsResponse> stats = new HashMap<>();
        for (CheckResponse check : checks) {
            try {
                stats.put(check.id(), checkHistoryService.getStats(check.id(), 24));
            } catch (NotFoundException ignored) {
                // чек мог быть удалён между чтением списка и статистикой
            }
        }
        model.addAttribute("checks", checks);
        model.addAttribute("stats", stats);
    }
}
