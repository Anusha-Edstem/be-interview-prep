package com.edstem.interviewprep.shortlink.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ShortCodeGeneratorTest {

  private final ShortCodeGenerator generator = new ShortCodeGenerator();

  @Test
  void generatedCodesAreAtMostEightCharacters() {
    for (int run = 0; run < 1000; run++) {
      assertThat(generator.generate()).hasSizeLessThanOrEqualTo(8);
    }
  }

  @Test
  void generatedCodesUseOnlyUrlSafeCharacters() {
    for (int run = 0; run < 1000; run++) {
      assertThat(generator.generate()).matches("[A-Za-z0-9]+");
    }
  }

  @Test
  void generatedCodesCollideRarelyEnoughToBeUsableAsIdentifiers() {
    Set<String> codes = new HashSet<>();
    for (int run = 0; run < 10000; run++) {
      codes.add(generator.generate());
    }

    assertThat(codes).hasSize(10000);
  }
}
