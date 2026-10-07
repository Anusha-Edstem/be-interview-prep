package com.edstem.interviewprep.auth.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.edstem.interviewprep.auth.dto.request.LoginRequest;
import com.edstem.interviewprep.auth.dto.request.RegisterRequest;
import com.edstem.interviewprep.auth.dto.response.AuthTokenResponse;
import com.edstem.interviewprep.auth.exception.EmailAlreadyRegisteredException;
import com.edstem.interviewprep.auth.exception.InvalidCredentialsException;
import com.edstem.interviewprep.auth.service.AuthService;
import com.edstem.interviewprep.common.security.JsonAccessDeniedHandler;
import com.edstem.interviewprep.common.security.JsonAuthenticationEntryPoint;
import com.edstem.interviewprep.common.security.SecurityConfig;
import com.edstem.interviewprep.user.dto.response.UserResponse;
import com.edstem.interviewprep.user.entity.Role;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JsonAuthenticationEntryPoint.class, JsonAccessDeniedHandler.class})
class AuthControllerTest {

  private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

  @Autowired private MockMvc mockMvc;

  @MockitoBean private AuthService authService;

  @Test
  void registerIsReachableWithoutAuthenticationAndReturnsTheCreatedProfile() throws Exception {
    when(authService.register(any(RegisterRequest.class)))
        .thenReturn(
            new UserResponse(
                USER_ID, "ada@example.com", Role.USER, Instant.parse("2026-01-01T00:00:00Z")));

    mockMvc
        .perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"ada@example.com\",\"password\":\"correct-horse-battery\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(USER_ID.toString()))
        .andExpect(jsonPath("$.email").value("ada@example.com"))
        .andExpect(jsonPath("$.role").value("USER"));
  }

  @Test
  void registerNeverEchoesThePasswordBack() throws Exception {
    when(authService.register(any(RegisterRequest.class)))
        .thenReturn(
            new UserResponse(
                USER_ID, "ada@example.com", Role.USER, Instant.parse("2026-01-01T00:00:00Z")));

    mockMvc
        .perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"ada@example.com\",\"password\":\"correct-horse-battery\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.password").doesNotExist())
        .andExpect(jsonPath("$.passwordHash").doesNotExist());
  }

  @Test
  void registerRejectsAMalformedEmailWithAFieldError() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"not-an-email\",\"password\":\"correct-horse-battery\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
        .andExpect(jsonPath("$.fieldErrors[0].field").value("email"))
        .andExpect(
            jsonPath("$.fieldErrors[0].message").value("email must be a well-formed address"));

    verify(authService, never()).register(any(RegisterRequest.class));
  }

  @Test
  void registerRejectsAPasswordBelowTheMinimumLength() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"ada@example.com\",\"password\":\"short\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
        .andExpect(jsonPath("$.fieldErrors[0].field").value("password"))
        .andExpect(
            jsonPath("$.fieldErrors[0].message")
                .value("password must be between 12 and 72 characters"));
  }

  @Test
  void registerReportsAnAlreadyTakenEmailAsAConflict() throws Exception {
    when(authService.register(any(RegisterRequest.class)))
        .thenThrow(new EmailAlreadyRegisteredException());

    mockMvc
        .perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"ada@example.com\",\"password\":\"correct-horse-battery\"}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.status").value(409))
        .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_REGISTERED"));
  }

  @Test
  void loginIsReachableWithoutAuthenticationAndReturnsABearerToken() throws Exception {
    when(authService.login(any(LoginRequest.class)))
        .thenReturn(
            new AuthTokenResponse(
                "a.token.value", "Bearer", 900L, Instant.parse("2026-01-01T00:15:00Z")));

    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"ada@example.com\",\"password\":\"correct-horse-battery\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").value("a.token.value"))
        .andExpect(jsonPath("$.tokenType").value("Bearer"))
        .andExpect(jsonPath("$.expiresInSeconds").value(900))
        .andExpect(jsonPath("$.expiresAt").value("2026-01-01T00:15:00Z"));
  }

  @Test
  void loginReportsBadCredentialsAsJsonUnauthorized() throws Exception {
    when(authService.login(any(LoginRequest.class))).thenThrow(new InvalidCredentialsException());

    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"ada@example.com\",\"password\":\"wrong-password\"}"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.status").value(401))
        .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
        .andExpect(jsonPath("$.message").value("Email or password is incorrect"));
  }
}
