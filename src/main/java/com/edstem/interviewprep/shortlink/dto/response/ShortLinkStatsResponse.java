package com.edstem.interviewprep.shortlink.dto.response;

import java.time.Instant;

public record ShortLinkStatsResponse(
    String code,
    String originalUrl,
    long visitCount,
    Instant createdDate,
    Instant expiresAt,
    boolean expired) {}
