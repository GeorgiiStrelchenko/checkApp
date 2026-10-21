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

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class UiController {

    private final CheckService checkService;
    private final CheckHistoryService checkHistoryService;
    private final UserService userService;

    @GetMapping("/")
    public String dashboard(Model model) {
        if (!model.containsAttribute("checkRequest")) {
            model.addAttribute("checkRequest", new CheckRequest("", "", 60, true));
        }
        populateDashboard(model);
        return "dashboard";
    }

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

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String registerForm(Model model) {
        if (!model.containsAttribute("registerRequest")) {
            model.addAttribute("registerRequest", new RegisterRequest("", ""));
        }
        return "register";
    }

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
