package com.example.checkApp.dto.request;

import jakarta.validation.constraints.NotBlank;

import org.hibernate.validator.constraints.URL;

public record PingRequest(
        @NotBlank(message = "url must not be blank")
        @URL(message = "url must be a valid URL")
        String url
) {
}
