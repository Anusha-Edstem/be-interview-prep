package com.edstem.interviewprep.shortlink.service;

import com.edstem.interviewprep.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class ShortCodeExhaustedException extends ApiException {

  public ShortCodeExhaustedException(int attempts) {
    super(
        HttpStatus.SERVICE_UNAVAILABLE,
        "SHORT_CODE_UNAVAILABLE",
        "Could not allocate an unused short code in " + attempts + " attempts");
  }
}
