package com.edstem.interviewprep.shortlink.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.edstem.interviewprep.shortlink.dto.request.CreateShortLinkRequest;
import com.edstem.interviewprep.shortlink.dto.response.ShortLinkResponse;
import com.edstem.interviewprep.shortlink.entity.ShortLink;
import com.edstem.interviewprep.shortlink.repository.ShortLinkRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ShortLinkVisitCountConcurrencyTest {

  private static final int CONCURRENT_VISITS = 50;
  private static final int THREADS = 16;

  @Autowired private ShortLinkService shortLinkService;

  @Autowired private ShortLinkRepository shortLinkRepository;

  @Test
  void everyConcurrentVisitIsCountedExactlyOnce() throws Exception {
    ShortLinkResponse created =
        shortLinkService.createShortLink(
            new CreateShortLinkRequest("https://example.com/busy", null));
    ExecutorService pool = Executors.newFixedThreadPool(THREADS);
    CountDownLatch readyToStart = new CountDownLatch(1);
    List<Future<String>> visits = new ArrayList<>();

    for (int visit = 0; visit < CONCURRENT_VISITS; visit++) {
      visits.add(
          pool.submit(
              () -> {
                readyToStart.await();
                return shortLinkService.resolveForRedirect(created.code());
              }));
    }
    readyToStart.countDown();
    for (Future<String> visit : visits) {
      assertThat(visit.get(30, TimeUnit.SECONDS)).isEqualTo("https://example.com/busy");
    }
    pool.shutdown();
    assertThat(pool.awaitTermination(30, TimeUnit.SECONDS)).isTrue();

    ShortLink reloaded = shortLinkRepository.findByCode(created.code()).orElseThrow();
    assertThat(reloaded.getVisitCount()).isEqualTo(CONCURRENT_VISITS);
  }
}
