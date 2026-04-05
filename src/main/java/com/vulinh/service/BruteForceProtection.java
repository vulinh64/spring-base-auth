package com.vulinh.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Component;

@Component
public class BruteForceProtection {

  private static final int MAX_ATTEMPTS = 5;
  private static final Duration LOCKOUT_DURATION = Duration.ofMinutes(5);

  private final Cache<String, AtomicInteger> attemptsCache =
      Caffeine.newBuilder().expireAfterWrite(LOCKOUT_DURATION).build();

  public void checkLocked(String username) {
    var attempts = attemptsCache.getIfPresent(username);

    if (attempts != null && attempts.get() >= MAX_ATTEMPTS) {
      throw new IllegalStateException(
          "Account is temporarily locked due to too many failed login attempts");
    }
  }

  public void recordFailure(String username) {
    attemptsCache.get(username, _ -> new AtomicInteger()).incrementAndGet();
  }

  public void recordSuccess(String username) {
    attemptsCache.invalidate(username);
  }
}
