package com.edstem.interviewprep.auth.controller;

import com.edstem.interviewprep.auth.dto.request.LoginRequest;
import com.edstem.interviewprep.auth.dto.request.RegisterRequest;
import com.edstem.interviewprep.auth.dto.response.AuthTokenResponse;
import com.edstem.interviewprep.auth.service.AuthService;
import com.edstem.interviewprep.user.dto.response.UserResponse;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

  private final AuthService authService;

  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  @PostMapping("/register")
  public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
    UserResponse registered = authService.register(request);
    return ResponseEntity.created(URI.create("/api/v1/users/me")).body(registered);
  }

  @PostMapping("/login")
  public ResponseEntity<AuthTokenResponse> login(@Valid @RequestBody LoginRequest request) {
    return ResponseEntity.ok(authService.login(request));
  }
}
