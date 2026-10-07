package com.edstem.interviewprep.shortlink.service;

import java.security.SecureRandom;
import org.springframework.stereotype.Component;

@Component
public class ShortCodeGenerator {

  private static final String ALPHABET =
      "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
  private static final int CODE_LENGTH = 7;

  private final SecureRandom random = new SecureRandom();

  public String generate() {
    StringBuilder code = new StringBuilder(CODE_LENGTH);
    for (int position = 0; position < CODE_LENGTH; position++) {
      code.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
    }
    return code.toString();
  }
}
