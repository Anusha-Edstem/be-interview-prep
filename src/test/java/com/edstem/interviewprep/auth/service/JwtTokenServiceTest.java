package com.edstem.interviewprep.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.edstem.interviewprep.auth.dto.response.AuthTokenResponse;
import com.edstem.interviewprep.user.entity.Role;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

class JwtTokenServiceTest {

  private static final String SECRET = "a-test-signing-key-of-at-least-32-bytes-long";
  private static final SecretKeySpec KEY = new SecretKeySpec(SECRET.getBytes(), "HmacSHA256");
  private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
  private static final UUID ADMIN_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

  private final NimbusJwtEncoder encoder = new NimbusJwtEncoder(new ImmutableSecret<>(KEY));
  private final JwtDecoder decoder =
      NimbusJwtDecoder.withSecretKey(KEY).macAlgorithm(MacAlgorithm.HS256).build();

  private JwtTokenService tokenServiceWithLifetime(Duration lifetime) {
    return new JwtTokenService(encoder, "be-interview-prep-test", lifetime);
  }

  @Test
  void anIssuedTokenExpiresFifteenMinutesAfterItIsIssued() {
    AuthTokenResponse issued =
        tokenServiceWithLifetime(Duration.ofMinutes(15)).issueToken(USER_ID, Role.USER);

    Jwt decoded = decoder.decode(issued.accessToken());

    assertThat(Duration.between(decoded.getIssuedAt(), decoded.getExpiresAt()))
        .isEqualTo(Duration.ofMinutes(15));
    assertThat(issued.expiresInSeconds()).isEqualTo(900L);
    assertThat(issued.tokenType()).isEqualTo("Bearer");
  }

  @Test
  void anIssuedTokenCarriesTheRoleSoAuthorisationCanReadIt() {
    AuthTokenResponse issued =
        tokenServiceWithLifetime(Duration.ofMinutes(15)).issueToken(ADMIN_ID, Role.ADMIN);

    Jwt decoded = decoder.decode(issued.accessToken());

    assertThat(decoded.getSubject()).isEqualTo(ADMIN_ID.toString());
    assertThat(decoded.getClaimAsStringList("roles")).containsExactly("ADMIN");
    assertThat(decoded.getClaimAsString("iss")).isEqualTo("be-interview-prep-test");
  }

  @Test
  void aTokenIsAcceptedWhileItIsInsideItsFifteenMinuteWindow() {
    String justIssued = tokenIssuedAt(Instant.now().minus(14, ChronoUnit.MINUTES));

    Jwt decoded = decoder.decode(justIssued);

    assertThat(decoded.getSubject()).isEqualTo(USER_ID.toString());
  }

  @Test
  void aTokenIsRejectedOnceFifteenMinutesHavePassedSinceItWasIssued() {
    String stale = tokenIssuedAt(Instant.now().minus(16, ChronoUnit.MINUTES));

    assertThatThrownBy(() -> decoder.decode(stale)).isInstanceOf(JwtValidationException.class);
  }

  private String tokenIssuedAt(Instant issuedAt) {
    JwtClaimsSet claims =
        JwtClaimsSet.builder()
            .issuer("be-interview-prep-test")
            .issuedAt(issuedAt)
            .expiresAt(issuedAt.plus(Duration.ofMinutes(15)))
            .subject(USER_ID.toString())
            .claim("roles", List.of(Role.USER.name()))
            .build();
    return encoder
        .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
        .getTokenValue();
  }

  @Test
  void aTokenSignedWithADifferentKeyIsRejected() {
    SecretKeySpec otherKey =
        new SecretKeySpec("a-different-signing-key-also-32-bytes-long".getBytes(), "HmacSHA256");
    AuthTokenResponse foreign =
        new JwtTokenService(
                new NimbusJwtEncoder(new ImmutableSecret<>(otherKey)),
                "be-interview-prep-test",
                Duration.ofMinutes(15))
            .issueToken(ADMIN_ID, Role.ADMIN);

    assertThatThrownBy(() -> decoder.decode(foreign.accessToken()))
        .isInstanceOf(org.springframework.security.oauth2.jwt.BadJwtException.class);
  }
}
