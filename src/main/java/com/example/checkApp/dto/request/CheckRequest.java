package com.example.checkApp.dto.request;

import org.hibernate.validator.constraints.URL;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CheckRequest(
        @NotBlank(message = "name must not be blank")
        @Size(max = 255, message = "name must be at most 255 characters")
        String name,

        @NotBlank(message = "url must not be blank")
        @URL(message = "url must be a valid URL")
        @Size(max = 2048, message = "url must be at most 2048 characters")
        String url,

        @NotNull(message = "checkIntervalSeconds must not be null")
        @Min(value = 10, message = "checkIntervalSeconds must be at least 10")
        @Max(value = 86400, message = "checkIntervalSeconds must be at most 86400")
        Integer checkIntervalSeconds,

        @NotNull(message = "enabled must not be null")
        Boolean enabled
) {
}
