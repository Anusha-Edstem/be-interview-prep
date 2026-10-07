package com.edstem.interviewprep.shortlink.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.edstem.interviewprep.shortlink.dto.request.CreateShortLinkRequest;
import com.edstem.interviewprep.shortlink.dto.response.ShortLinkResponse;
import com.edstem.interviewprep.shortlink.dto.response.ShortLinkStatsResponse;
import com.edstem.interviewprep.shortlink.exception.ShortLinkExpiredException;
import com.edstem.interviewprep.shortlink.exception.ShortLinkNotFoundException;
import com.edstem.interviewprep.shortlink.service.ShortLinkService;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({ShortLinkController.class, ShortLinkRedirectController.class})
class ShortLinkControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private ShortLinkService shortLinkService;

  @Test
  void createShortLinkReturnsCreatedWithTheCodeAndShortUrl() throws Exception {
    when(shortLinkService.createShortLink(any(CreateShortLinkRequest.class)))
        .thenReturn(
            new ShortLinkResponse(
                "abc1234",
                "http://short.test/s/abc1234",
                "https://example.com/page",
                null,
                Instant.parse("2026-01-01T00:00:00Z")));

    mockMvc
        .perform(
            post("/api/v1/links")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"url\":\"https://example.com/page\"}"))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", "/api/v1/links/abc1234/stats"))
        .andExpect(jsonPath("$.code").value("abc1234"))
        .andExpect(jsonPath("$.shortUrl").value("http://short.test/s/abc1234"))
        .andExpect(jsonPath("$.originalUrl").value("https://example.com/page"));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "not-a-url",
        "javascript:alert(1)",
        "ftp://example.com/file",
        "/relative/path",
        "http://"
      })
  void createShortLinkRejectsAUrlThatIsNotAbsoluteHttpOrHttps(String url) throws Exception {
    mockMvc
        .perform(
            post("/api/v1/links")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"url\":\"" + url + "\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
        .andExpect(jsonPath("$.fieldErrors[0].field").value("url"))
        .andExpect(
            jsonPath("$.fieldErrors[0].message").value("must be an absolute http or https URL"));
  }

  @Test
  void createShortLinkRejectsABlankUrl() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/links")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"url\":\"   \"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
        .andExpect(jsonPath("$.fieldErrors[0].field").value("url"))
        .andExpect(jsonPath("$.fieldErrors[0].message").value("url is required"));
  }

  @Test
  void createShortLinkRejectsAnExpiryInThePast() throws Exception {
    String body =
        "{\"url\":\"https://example.com\",\"expiresAt\":\""
            + Instant.now().minusSeconds(60)
            + "\"}";

    mockMvc
        .perform(post("/api/v1/links").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
        .andExpect(jsonPath("$.fieldErrors[0].field").value("expiresAt"))
        .andExpect(jsonPath("$.fieldErrors[0].message").value("expiresAt must be in the future"));
  }

  @Test
  void visitingAShortCodeRedirectsToTheOriginalUrlWithoutCaching() throws Exception {
    when(shortLinkService.resolveForRedirect("abc1234")).thenReturn("https://example.com/page?a=1");

    mockMvc
        .perform(get("/s/{code}", "abc1234"))
        .andExpect(status().isFound())
        .andExpect(header().string("Location", "https://example.com/page?a=1"))
        .andExpect(header().string("Cache-Control", "no-store"));
  }

  @Test
  void visitingAnUnknownCodeReturnsNotFound() throws Exception {
    when(shortLinkService.resolveForRedirect("missing"))
        .thenThrow(new ShortLinkNotFoundException("missing"));

    mockMvc
        .perform(get("/s/{code}", "missing"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("SHORT_LINK_NOT_FOUND"))
        .andExpect(jsonPath("$.path").value("/s/missing"));
  }

  @Test
  void visitingAnExpiredCodeReturnsGone() throws Exception {
    when(shortLinkService.resolveForRedirect("expired"))
        .thenThrow(new ShortLinkExpiredException("expired"));

    mockMvc
        .perform(get("/s/{code}", "expired"))
        .andExpect(status().isGone())
        .andExpect(jsonPath("$.status").value(410))
        .andExpect(jsonPath("$.code").value("SHORT_LINK_EXPIRED"));
  }

  @Test
  void statsReturnTheOriginalUrlVisitCountAndCreatedDate() throws Exception {
    when(shortLinkService.getStats("abc1234"))
        .thenReturn(
            new ShortLinkStatsResponse(
                "abc1234",
                "https://example.com/page",
                42L,
                Instant.parse("2026-01-01T00:00:00Z"),
                null,
                false));

    mockMvc
        .perform(get("/api/v1/links/{code}/stats", "abc1234"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.originalUrl").value("https://example.com/page"))
        .andExpect(jsonPath("$.visitCount").value(42))
        .andExpect(jsonPath("$.createdDate").value("2026-01-01T00:00:00Z"))
        .andExpect(jsonPath("$.expired").value(false));
  }

  @Test
  void statsForAnUnknownCodeReturnNotFound() throws Exception {
    when(shortLinkService.getStats("missing")).thenThrow(new ShortLinkNotFoundException("missing"));

    mockMvc
        .perform(get("/api/v1/links/{code}/stats", "missing"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("SHORT_LINK_NOT_FOUND"));
  }
}
