package com.edstem.interviewprep.shortlink.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.edstem.interviewprep.shortlink.dto.request.CreateShortLinkRequest;
import com.edstem.interviewprep.shortlink.dto.response.ShortLinkResponse;
import com.edstem.interviewprep.shortlink.dto.response.ShortLinkStatsResponse;
import com.edstem.interviewprep.shortlink.entity.ShortLink;
import com.edstem.interviewprep.shortlink.exception.ShortLinkExpiredException;
import com.edstem.interviewprep.shortlink.exception.ShortLinkNotFoundException;
import com.edstem.interviewprep.shortlink.repository.ShortLinkRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShortLinkServiceTest {

  @Mock private ShortLinkRepository shortLinkRepository;

  @Mock private ShortCodeGenerator shortCodeGenerator;

  private ShortLinkService shortLinkService;

  @BeforeEach
  void setUp() {
    shortLinkService =
        new ShortLinkService(shortLinkRepository, shortCodeGenerator, "http://short.test/");
  }

  @Test
  void shorteningTheSameUrlTwiceProducesTwoIndependentLinks() {
    when(shortCodeGenerator.generate()).thenReturn("aaaaaaa", "bbbbbbb");
    when(shortLinkRepository.existsByCode(anyString())).thenReturn(false);
    when(shortLinkRepository.save(any(ShortLink.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    CreateShortLinkRequest request = new CreateShortLinkRequest("https://example.com/page", null);

    ShortLinkResponse first = shortLinkService.createShortLink(request);
    ShortLinkResponse second = shortLinkService.createShortLink(request);

    assertThat(first.code()).isNotEqualTo(second.code());
    assertThat(first.originalUrl()).isEqualTo(second.originalUrl());
  }

  @Test
  void createdShortUrlJoinsTheBaseUrlWithoutDoublingTheSlash() {
    when(shortCodeGenerator.generate()).thenReturn("abc1234");
    when(shortLinkRepository.existsByCode("abc1234")).thenReturn(false);
    when(shortLinkRepository.save(any(ShortLink.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    ShortLinkResponse response =
        shortLinkService.createShortLink(
            new CreateShortLinkRequest("https://example.com/page", null));

    assertThat(response.shortUrl()).isEqualTo("http://short.test/s/abc1234");
  }

  @Test
  void allocatingACodeRetriesWhenTheFirstCandidateIsTaken() {
    when(shortCodeGenerator.generate()).thenReturn("taken11", "free123");
    when(shortLinkRepository.existsByCode("taken11")).thenReturn(true);
    when(shortLinkRepository.existsByCode("free123")).thenReturn(false);
    when(shortLinkRepository.save(any(ShortLink.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    ShortLinkResponse response =
        shortLinkService.createShortLink(new CreateShortLinkRequest("https://example.com", null));

    assertThat(response.code()).isEqualTo("free123");
  }

  @Test
  void allocatingACodeGivesUpAfterTheAttemptLimit() {
    when(shortCodeGenerator.generate()).thenReturn("taken11");
    when(shortLinkRepository.existsByCode("taken11")).thenReturn(true);

    assertThatThrownBy(
            () ->
                shortLinkService.createShortLink(
                    new CreateShortLinkRequest("https://example.com", null)))
        .isInstanceOf(ShortCodeExhaustedException.class);
    verify(shortLinkRepository, never()).save(any(ShortLink.class));
  }

  @Test
  void resolvingAnUnknownCodeThrowsNotFound() {
    when(shortLinkRepository.findByCode("missing")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> shortLinkService.resolveForRedirect("missing"))
        .isInstanceOf(ShortLinkNotFoundException.class);
    verify(shortLinkRepository, never()).incrementVisitCount(anyString());
  }

  @Test
  void resolvingAnExpiredCodeThrowsGoneAndDoesNotCountAVisit() {
    ShortLink expired =
        ShortLink.create(
            "expired", "https://example.com", Instant.now().minus(1, ChronoUnit.MINUTES));
    when(shortLinkRepository.findByCode("expired")).thenReturn(Optional.of(expired));

    assertThatThrownBy(() -> shortLinkService.resolveForRedirect("expired"))
        .isInstanceOf(ShortLinkExpiredException.class);
    verify(shortLinkRepository, never()).incrementVisitCount(anyString());
  }

  @Test
  void resolvingALiveCodeReturnsTheOriginalUrlAndCountsOneVisit() {
    ShortLink link =
        ShortLink.create("live123", "https://example.com/page", Instant.now().plusSeconds(600));
    when(shortLinkRepository.findByCode("live123")).thenReturn(Optional.of(link));

    String originalUrl = shortLinkService.resolveForRedirect("live123");

    assertThat(originalUrl).isEqualTo("https://example.com/page");
    verify(shortLinkRepository).incrementVisitCount("live123");
  }

  @Test
  void statsReportTheLinkAsExpiredOnceItsExpiryHasPassed() {
    ShortLink expired =
        ShortLink.create(
            "expired", "https://example.com", Instant.now().minus(1, ChronoUnit.MINUTES));
    when(shortLinkRepository.findByCode("expired")).thenReturn(Optional.of(expired));

    ShortLinkStatsResponse stats = shortLinkService.getStats("expired");

    assertThat(stats.expired()).isTrue();
    assertThat(stats.visitCount()).isZero();
  }

  @Test
  void statsForAnUnknownCodeThrowNotFound() {
    when(shortLinkRepository.findByCode("missing")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> shortLinkService.getStats("missing"))
        .isInstanceOf(ShortLinkNotFoundException.class);
  }
}
