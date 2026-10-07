package com.edstem.interviewprep.shortlink.exception;

import com.edstem.interviewprep.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class ShortLinkExpiredException extends ApiException {

  public ShortLinkExpiredException(String code) {
    super(HttpStatus.GONE, "SHORT_LINK_EXPIRED", "The short link " + code + " has expired");
  }
}
