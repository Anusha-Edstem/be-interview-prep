package com.edstem.interviewprep.user.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.edstem.interviewprep.common.dto.response.PageResponse;
import com.edstem.interviewprep.common.security.JsonAccessDeniedHandler;
import com.edstem.interviewprep.common.security.JsonAuthenticationEntryPoint;
import com.edstem.interviewprep.common.security.SecurityConfig;
import com.edstem.interviewprep.user.dto.response.UserResponse;
import com.edstem.interviewprep.user.entity.Role;
import com.edstem.interviewprep.user.service.UserService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, JsonAuthenticationEntryPoint.class, JsonAccessDeniedHandler.class})
class UserControllerTest {

  private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
  private static final UUID ADMIN_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

  @Autowired private MockMvc mockMvc;

  @MockitoBean private UserService userService;

  private RequestPostProcessor callerWithRole(UUID id, Role role) {
    return jwt()
        .jwt(token -> token.subject(id.toString()).claim("roles", List.of(role.name())))
        .authorities(new SimpleGrantedAuthority("ROLE_" + role.name()));
  }

  private UserResponse profileOf(UUID id, String email, Role role) {
    return new UserResponse(id, email, role, Instant.parse("2026-01-01T00:00:00Z"));
  }

  @Test
  void anAuthenticatedUserCanReadTheirOwnProfile() throws Exception {
    when(userService.getProfile(USER_ID))
        .thenReturn(profileOf(USER_ID, "ada@example.com", Role.USER));

    mockMvc
        .perform(get("/api/v1/users/me").with(callerWithRole(USER_ID, Role.USER)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(USER_ID.toString()))
        .andExpect(jsonPath("$.email").value("ada@example.com"))
        .andExpect(jsonPath("$.role").value("USER"));
  }

  @Test
  void theProfileEndpointReadsTheIdFromTheTokenRatherThanTheRequest() throws Exception {
    when(userService.getProfile(ADMIN_ID))
        .thenReturn(profileOf(ADMIN_ID, "root@example.com", Role.ADMIN));

    mockMvc
        .perform(
            get("/api/v1/users/me")
                .param("id", USER_ID.toString())
                .with(callerWithRole(ADMIN_ID, Role.ADMIN)))
        .andExpect(status().isOk());

    verify(userService).getProfile(ADMIN_ID);
    verify(userService, never()).getProfile(USER_ID);
  }

  @Test
  void anUnauthenticatedProfileRequestIsRejectedAsJson() throws Exception {
    mockMvc
        .perform(get("/api/v1/users/me"))
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.status").value(401))
        .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"))
        .andExpect(jsonPath("$.path").value("/api/v1/users/me"))
        .andExpect(jsonPath("$.timestamp").exists());

    verify(userService, never()).getProfile(any(UUID.class));
  }

  @Test
  void anUnauthenticatedListRequestIsRejectedAsJson() throws Exception {
    mockMvc
        .perform(get("/api/v1/users"))
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));

    verify(userService, never()).listUsers(any(Pageable.class));
  }

  @Test
  void aUserIsForbiddenFromListingAllUsers() throws Exception {
    mockMvc
        .perform(get("/api/v1/users").with(callerWithRole(USER_ID, Role.USER)))
        .andExpect(status().isForbidden())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.status").value(403))
        .andExpect(jsonPath("$.code").value("ACCESS_DENIED"))
        .andExpect(jsonPath("$.path").value("/api/v1/users"));

    verify(userService, never()).listUsers(any(Pageable.class));
  }

  @Test
  void anAdminCanListAllUsers() throws Exception {
    when(userService.listUsers(any(Pageable.class)))
        .thenReturn(
            new PageResponse<>(
                List.of(
                    profileOf(ADMIN_ID, "root@example.com", Role.ADMIN),
                    profileOf(USER_ID, "ada@example.com", Role.USER)),
                0,
                20,
                2,
                1,
                true));

    mockMvc
        .perform(get("/api/v1/users").with(callerWithRole(ADMIN_ID, Role.ADMIN)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(2))
        .andExpect(jsonPath("$.totalElements").value(2))
        .andExpect(jsonPath("$.content[0].email").value("root@example.com"));
  }

  @Test
  void aTokenWithoutTheAdminRoleCannotReachTheAdminEndpointEvenWhenOtherwiseValid()
      throws Exception {
    mockMvc
        .perform(
            get("/api/v1/users")
                .with(
                    jwt()
                        .jwt(token -> token.subject(USER_ID.toString()).claim("roles", List.of()))
                        .authorities(List.of())))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
  }
}
