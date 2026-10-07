package com.edstem.interviewprep.shortlink.mapper;

import com.edstem.interviewprep.shortlink.dto.response.ShortLinkResponse;
import com.edstem.interviewprep.shortlink.dto.response.ShortLinkStatsResponse;
import com.edstem.interviewprep.shortlink.entity.ShortLink;
import java.time.Instant;

public final class ShortLinkMapper {

  private ShortLinkMapper() {}

  public static ShortLinkResponse toResponse(ShortLink link, String shortUrl) {
    return new ShortLinkResponse(
        link.getCode(),
        shortUrl,
        link.getOriginalUrl(),
        link.getExpiresAt(),
        link.getCreatedDate());
  }

  public static ShortLinkStatsResponse toStatsResponse(ShortLink link, Instant moment) {
    return new ShortLinkStatsResponse(
        link.getCode(),
        link.getOriginalUrl(),
        link.getVisitCount(),
        link.getCreatedDate(),
        link.getExpiresAt(),
        link.hasExpiredAt(moment));
  }
}
