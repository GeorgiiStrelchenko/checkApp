package com.example.checkApp.client;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class HttpClientService {

    private final RestClient restClient;

    public HttpClientService(RestClient restClient) {
        this.restClient = restClient;
    }

    public PingOutcome ping(String url) {
        long start = System.nanoTime();
        try {
            Integer httpCode = restClient.get()
                    .uri(url)
                    .exchange((request, response) -> response.getStatusCode().value());
            return new PingOutcome(httpCode, elapsedMs(start), null);
        } catch (RestClientException e) {
            return new PingOutcome(null, elapsedMs(start), e.getMessage());
        }
    }

    private long elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }
}
