package com.example.checkApp.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.checkApp.dto.request.PingRequest;
import com.example.checkApp.dto.response.PingResponse;
import com.example.checkApp.service.PingService;

import jakarta.validation.Valid;

@Tag(name = "Ping", description = "One-shot URL availability probe")
@RestController
@RequestMapping("/api")
public class PingController {

    private final PingService pingService;

    public PingController(PingService pingService) {
        this.pingService = pingService;
    }

    @PostMapping("/ping")
    public PingResponse ping(@Valid @RequestBody PingRequest request) {
        return pingService.ping(request.url());
    }
}
