package com.edstem.interviewprep.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.edstem.interviewprep.auth.service.JwtTokenService;
import com.edstem.interviewprep.user.entity.Role;
import com.edstem.interviewprep.user.entity.User;
import com.edstem.interviewprep.user.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class AuthFlowIntegrationTest {

  private static final String PASSWORD = "correct-horse-battery";
  private static final Duration TOKEN_LIFETIME = Duration.ofMinutes(15);

  @Autowired private MockMvc mockMvc;

  @Autowired private UserRepository userRepository;

  @Autowired private PasswordEncoder passwordEncoder;

  @Autowired private JwtEncoder jwtEncoder;

  @Autowired private ObjectMapper objectMapper;

  private String registerAndLogin(String email) throws Exception {
    mockMvc
        .perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(credentials(email)))
        .andExpect(status().isCreated());
    String loginBody =
        mockMvc
            .perform(
                post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(credentials(email)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    JsonNode token = objectMapper.readTree(loginBody);
    assertThat(token.get("tokenType").asText()).isEqualTo("Bearer");
    return token.get("accessToken").asText();
  }

  private String credentials(String email) {
    return "{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}";
  }

  private String bearer(String token) {
    return "Bearer " + token;
  }

  private String adminToken(String email) {
    User admin =
        userRepository.save(User.create(email, passwordEncoder.encode(PASSWORD), Role.ADMIN));
    return new JwtTokenService(jwtEncoder, "be-interview-prep-test", Duration.ofMinutes(15))
        .issueToken(admin.getId(), admin.getRole())
        .accessToken();
  }

  @Test
  void aUserCanRegisterLogInAndReadTheirOwnProfile() throws Exception {
    String token = registerAndLogin("flow-user@example.com");

    mockMvc
        .perform(get("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value("flow-user@example.com"))
        .andExpect(jsonPath("$.role").value("USER"))
        .andExpect(jsonPath("$.passwordHash").doesNotExist());
  }

  @Test
  void aRegisteredUserIsDeniedTheAdminEndpointWithTheirRealToken() throws Exception {
    String token = registerAndLogin("denied-user@example.com");

    mockMvc
        .perform(get("/api/v1/users").header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isForbidden())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.status").value(403))
        .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
  }

  @Test
  void anAdminCanListUsersWithTheirRealToken() throws Exception {
    String token = adminToken("flow-admin@example.com");

    mockMvc
        .perform(get("/api/v1/users").header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.totalElements").isNumber());
  }

  @Test
  void theStoredPasswordIsAHashAndNeverThePlaintext() throws Exception {
    registerAndLogin("hashed-user@example.com");

    User stored = userRepository.findByEmail("hashed-user@example.com").orElseThrow();

    assertThat(stored.getPasswordHash()).isNotEqualTo(PASSWORD);
    assertThat(stored.getPasswordHash()).startsWith("{bcrypt}$2");
    assertThat(passwordEncoder.matches(PASSWORD, stored.getPasswordHash())).isTrue();
  }

  @Test
  void aRequestWithNoTokenIsRejectedAsJsonUnauthorized() throws Exception {
    mockMvc
        .perform(get("/api/v1/users/me"))
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
  }

  @Test
  void aGarbledTokenIsRejectedAsJsonUnauthorized() throws Exception {
    mockMvc
        .perform(get("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, bearer("not.a.token")))
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
  }

  @Test
  void aTokenIsStillAcceptedFourteenMinutesAfterItWasIssued() throws Exception {
    User user =
        userRepository.save(
            User.create("fresh-user@example.com", passwordEncoder.encode(PASSWORD), Role.USER));

    mockMvc
        .perform(
            get("/api/v1/users/me")
                .header(
                    HttpHeaders.AUTHORIZATION,
                    bearer(tokenIssuedAt(user, Instant.now().minus(14, ChronoUnit.MINUTES)))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value("fresh-user@example.com"));
  }

  @Test
  void aTokenOlderThanFifteenMinutesIsRejectedAsJsonUnauthorized() throws Exception {
    User user =
        userRepository.save(
            User.create("expired-user@example.com", passwordEncoder.encode(PASSWORD), Role.USER));

    mockMvc
        .perform(
            get("/api/v1/users/me")
                .header(
                    HttpHeaders.AUTHORIZATION,
                    bearer(tokenIssuedAt(user, Instant.now().minus(16, ChronoUnit.MINUTES)))))
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
  }

  private String tokenIssuedAt(User user, Instant issuedAt) {
    JwtClaimsSet claims =
        JwtClaimsSet.builder()
            .issuer("be-interview-prep-test")
            .issuedAt(issuedAt)
            .expiresAt(issuedAt.plus(TOKEN_LIFETIME))
            .subject(user.getId().toString())
            .claim("roles", List.of(user.getRole().name()))
            .build();
    return jwtEncoder
        .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
        .getTokenValue();
  }

  @Test
  void theQ1AndQ2EndpointsStayReachableWithoutAToken() throws Exception {
    mockMvc.perform(get("/api/v1/tasks")).andExpect(status().isOk());
    mockMvc.perform(get("/s/{code}", "nosuchc")).andExpect(status().isNotFound());
  }
}
