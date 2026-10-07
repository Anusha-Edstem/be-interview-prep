package com.edstem.interviewprep.auth.service;

import com.edstem.interviewprep.auth.dto.response.AuthTokenResponse;
import com.edstem.interviewprep.user.entity.Role;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Service
public class JwtTokenService {

  private static final String TOKEN_TYPE = "Bearer";
  private static final String ROLES_CLAIM = "roles";

  private final JwtEncoder jwtEncoder;
  private final String issuer;
  private final Duration tokenLifetime;

  public JwtTokenService(
      JwtEncoder jwtEncoder,
      @Value("${app.auth.issuer}") String issuer,
      @Value("${app.auth.token-lifetime}") Duration tokenLifetime) {
    this.jwtEncoder = jwtEncoder;
    this.issuer = issuer;
    this.tokenLifetime = tokenLifetime;
  }

  public AuthTokenResponse issueToken(UUID subjectId, Role role) {
    Instant issuedAt = Instant.now();
    Instant expiresAt = issuedAt.plus(tokenLifetime);
    JwtClaimsSet claims =
        JwtClaimsSet.builder()
            .issuer(issuer)
            .issuedAt(issuedAt)
            .expiresAt(expiresAt)
            .subject(subjectId.toString())
            .claim(ROLES_CLAIM, List.of(role.name()))
            .build();
    JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
    String accessToken =
        jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    return new AuthTokenResponse(accessToken, TOKEN_TYPE, tokenLifetime.toSeconds(), expiresAt);
  }
}
