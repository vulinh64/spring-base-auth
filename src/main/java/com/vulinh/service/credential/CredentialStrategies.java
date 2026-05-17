package com.vulinh.service.credential;

import com.vulinh.data.entity.AccountCredential.CredentialType;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.vulinh.utils.CredentialStrategy;
import org.springframework.stereotype.Component;

@Component
public class CredentialStrategies {

  private final Map<CredentialType, CredentialStrategy> byType;

  public CredentialStrategies(List<CredentialStrategy> strategies) {
    byType =
        strategies.stream()
            .collect(
                Collectors.toUnmodifiableMap(CredentialStrategy::supports, Function.identity()));
  }

  public CredentialStrategy forType(CredentialType type) {
    var strategy = byType.get(type);

    if (strategy == null) {
      throw new IllegalArgumentException("Unsupported credential type: " + type);
    }

    return strategy;
  }
}
