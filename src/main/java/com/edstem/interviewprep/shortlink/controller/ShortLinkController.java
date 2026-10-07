package com.edstem.interviewprep.shortlink.controller;

import com.edstem.interviewprep.shortlink.dto.request.CreateShortLinkRequest;
import com.edstem.interviewprep.shortlink.dto.response.ShortLinkResponse;
import com.edstem.interviewprep.shortlink.dto.response.ShortLinkStatsResponse;
import com.edstem.interviewprep.shortlink.service.ShortLinkService;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/links")
public class ShortLinkController {

  private final ShortLinkService shortLinkService;

  public ShortLinkController(ShortLinkService shortLinkService) {
    this.shortLinkService = shortLinkService;
  }

  @PostMapping
  public ResponseEntity<ShortLinkResponse> createShortLink(
      @Valid @RequestBody CreateShortLinkRequest request) {
    ShortLinkResponse created = shortLinkService.createShortLink(request);
    return ResponseEntity.created(URI.create("/api/v1/links/" + created.code() + "/stats"))
        .body(created);
  }

  @GetMapping("/{code}/stats")
  public ResponseEntity<ShortLinkStatsResponse> getStats(@PathVariable String code) {
    return ResponseEntity.ok(shortLinkService.getStats(code));
  }
}
