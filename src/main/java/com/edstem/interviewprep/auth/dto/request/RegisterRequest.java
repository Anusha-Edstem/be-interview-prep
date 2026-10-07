package com.edstem.interviewprep.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank(message = "email is required")
        @Email(message = "email must be a well-formed address")
        @Size(max = 255, message = "email must be at most 255 characters")
        String email,
    @NotBlank(message = "password is required")
        @Size(min = 12, max = 72, message = "password must be between 12 and 72 characters")
        String password) {}
