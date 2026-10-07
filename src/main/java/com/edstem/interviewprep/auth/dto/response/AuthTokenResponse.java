package com.edstem.interviewprep.auth.dto.response;

import java.time.Instant;

public record AuthTokenResponse(
    String accessToken, String tokenType, long expiresInSeconds, Instant expiresAt) {}
