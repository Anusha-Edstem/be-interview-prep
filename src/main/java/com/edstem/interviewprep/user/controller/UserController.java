package com.edstem.interviewprep.user.controller;

import com.edstem.interviewprep.common.dto.response.PageResponse;
import com.edstem.interviewprep.user.dto.response.UserResponse;
import com.edstem.interviewprep.user.service.UserService;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

  private final UserService userService;

  public UserController(UserService userService) {
    this.userService = userService;
  }

  @GetMapping("/me")
  public ResponseEntity<UserResponse> getOwnProfile(@AuthenticationPrincipal Jwt jwt) {
    return ResponseEntity.ok(userService.getProfile(UUID.fromString(jwt.getSubject())));
  }

  @GetMapping
  public ResponseEntity<PageResponse<UserResponse>> listUsers(
      @PageableDefault(size = 20, sort = "createdDate", direction = Sort.Direction.DESC)
          Pageable pageable) {
    return ResponseEntity.ok(userService.listUsers(pageable));
  }
}
