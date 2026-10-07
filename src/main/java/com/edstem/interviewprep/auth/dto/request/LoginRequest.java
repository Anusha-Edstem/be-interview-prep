package com.edstem.interviewprep.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
    @NotBlank(message = "email is required")
        @Size(max = 255, message = "email must be at most 255 characters")
        String email,
    @NotBlank(message = "password is required")
        @Size(max = 72, message = "password must be at most 72 characters")
        String password) {}
