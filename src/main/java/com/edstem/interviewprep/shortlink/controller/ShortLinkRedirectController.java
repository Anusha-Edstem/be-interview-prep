package com.edstem.interviewprep.shortlink.controller;

import com.edstem.interviewprep.shortlink.service.ShortLinkService;
import java.net.URI;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ShortLinkRedirectController {

  private final ShortLinkService shortLinkService;

  public ShortLinkRedirectController(ShortLinkService shortLinkService) {
    this.shortLinkService = shortLinkService;
  }

  @GetMapping("/s/{code}")
  public ResponseEntity<Void> redirectToOriginalUrl(@PathVariable String code) {
    String originalUrl = shortLinkService.resolveForRedirect(code);
    return ResponseEntity.status(HttpStatus.FOUND)
        .location(URI.create(originalUrl))
        .cacheControl(CacheControl.noStore())
        .build();
  }
}
