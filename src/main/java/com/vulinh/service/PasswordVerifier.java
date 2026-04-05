package com.vulinh.service;

import com.vulinh.annotation.ExecutionTime;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PasswordVerifier {

  private final PasswordEncoder passwordEncoder;

  @ExecutionTime
  @Cacheable(
      value = "passwords",
      key = "T(com.vulinh.service.PasswordVerifier).cacheKey(#rawPassword, #encodedPassword)",
      unless = "!#result")
  public boolean matches(String rawPassword, String encodedPassword) {
    return passwordEncoder.matches(rawPassword, encodedPassword);
  }

  @SuppressWarnings("unused")
  public static String cacheKey(String rawPassword, String encodedPassword) {
    try {
      var hash =
          MessageDigest.getInstance("SHA-256")
              .digest((rawPassword + encodedPassword).getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hash);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 not available", e);
    }
  }
}
