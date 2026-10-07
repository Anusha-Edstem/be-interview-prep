package com.edstem.interviewprep.shortlink.dto.response;

import java.time.Instant;

public record ShortLinkResponse(
    String code, String shortUrl, String originalUrl, Instant expiresAt, Instant createdDate) {}
