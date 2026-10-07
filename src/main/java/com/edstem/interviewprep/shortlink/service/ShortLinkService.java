package com.edstem.interviewprep.shortlink.service;

import com.edstem.interviewprep.shortlink.dto.request.CreateShortLinkRequest;
import com.edstem.interviewprep.shortlink.dto.response.ShortLinkResponse;
import com.edstem.interviewprep.shortlink.dto.response.ShortLinkStatsResponse;
import com.edstem.interviewprep.shortlink.entity.ShortLink;
import com.edstem.interviewprep.shortlink.exception.ShortLinkExpiredException;
import com.edstem.interviewprep.shortlink.exception.ShortLinkNotFoundException;
import com.edstem.interviewprep.shortlink.mapper.ShortLinkMapper;
import com.edstem.interviewprep.shortlink.repository.ShortLinkRepository;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShortLinkService {

  private static final int MAX_CODE_ATTEMPTS = 5;

  private final ShortLinkRepository shortLinkRepository;
  private final ShortCodeGenerator shortCodeGenerator;
  private final String baseUrl;

  public ShortLinkService(
      ShortLinkRepository shortLinkRepository,
      ShortCodeGenerator shortCodeGenerator,
      @Value("${app.short-link.base-url}") String baseUrl) {
    this.shortLinkRepository = shortLinkRepository;
    this.shortCodeGenerator = shortCodeGenerator;
    this.baseUrl = baseUrl;
  }

  @Transactional
  public ShortLinkResponse createShortLink(CreateShortLinkRequest request) {
    ShortLink link =
        ShortLink.create(allocateUnusedCode(), request.url().trim(), request.expiresAt());
    ShortLink saved = shortLinkRepository.save(link);
    return ShortLinkMapper.toResponse(saved, shortUrlFor(saved.getCode()));
  }

  @Transactional
  public String resolveForRedirect(String code) {
    ShortLink link = findLinkOrThrow(code);
    if (link.hasExpiredAt(Instant.now())) {
      throw new ShortLinkExpiredException(code);
    }
    String originalUrl = link.getOriginalUrl();
    shortLinkRepository.incrementVisitCount(code);
    return originalUrl;
  }

  @Transactional(readOnly = true)
  public ShortLinkStatsResponse getStats(String code) {
    return ShortLinkMapper.toStatsResponse(findLinkOrThrow(code), Instant.now());
  }

  private String allocateUnusedCode() {
    for (int attempt = 0; attempt < MAX_CODE_ATTEMPTS; attempt++) {
      String candidate = shortCodeGenerator.generate();
      if (!shortLinkRepository.existsByCode(candidate)) {
        return candidate;
      }
    }
    throw new ShortCodeExhaustedException(MAX_CODE_ATTEMPTS);
  }

  private ShortLink findLinkOrThrow(String code) {
    return shortLinkRepository
        .findByCode(code)
        .orElseThrow(() -> new ShortLinkNotFoundException(code));
  }

  private String shortUrlFor(String code) {
    return baseUrl.replaceAll("/+$", "") + "/s/" + code;
  }
}
