package com.edstem.interviewprep.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Set;

public class HttpUrlValidator implements ConstraintValidator<HttpUrl, String> {

  private static final Set<String> ALLOWED_SCHEMES = Set.of("http", "https");

  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {
    if (value == null || value.isBlank()) {
      return true;
    }
    try {
      URI uri = new URI(value.trim());
      return uri.isAbsolute()
          && uri.getScheme() != null
          && ALLOWED_SCHEMES.contains(uri.getScheme().toLowerCase(Locale.ROOT))
          && uri.getHost() != null
          && !uri.getHost().isBlank();
    } catch (URISyntaxException malformed) {
      return false;
    }
  }
}
