package com.example.checkApp.service;

import org.springframework.stereotype.Service;

import com.example.checkApp.client.HttpClientService;
import com.example.checkApp.client.PingOutcome;
import com.example.checkApp.dto.response.PingResponse;
import com.example.checkApp.model.CheckStatus;

@Service
public class PingService {

    private final HttpClientService httpClientService;

    public PingService(HttpClientService httpClientService) {
        this.httpClientService = httpClientService;
    }

    public PingResponse ping(String url) {
        PingOutcome outcome = httpClientService.ping(url);
        CheckStatus status = outcome.reachable() ? CheckStatus.UP : CheckStatus.DOWN;
        return new PingResponse(
                url,
                status,
                outcome.responseTimeMs(),
                outcome.httpCode(),
                outcome.errorMessage()
        );
    }
}
