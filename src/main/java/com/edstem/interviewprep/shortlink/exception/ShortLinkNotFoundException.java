package com.edstem.interviewprep.shortlink.exception;

import com.edstem.interviewprep.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class ShortLinkNotFoundException extends ApiException {

  public ShortLinkNotFoundException(String code) {
    super(HttpStatus.NOT_FOUND, "SHORT_LINK_NOT_FOUND", "No short link exists for code " + code);
  }
}
