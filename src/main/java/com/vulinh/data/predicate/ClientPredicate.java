package com.vulinh.data.predicate;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.vulinh.data.entity.QClient;
import java.util.UUID;

public final class ClientPredicate {

  private final BooleanBuilder predicate;

  public ClientPredicate() {
    predicate = new BooleanBuilder(QClient.client.enabled.eq(true));
  }

  public ClientPredicate byClientId(String clientId) {
    predicate.and(
        isUuid(clientId)
            ? QClient.client.id.eq(UUID.fromString(clientId))
            : QClient.client.clientId.eq(clientId));

    return this;
  }

  public ClientPredicate byServiceApiKeyHash(String serviceApiKeyHash) {
    predicate.and(QClient.client.serviceApiKeyHash.eq(serviceApiKeyHash));

    return this;
  }

  public Predicate toPredicate() {
    return predicate;
  }

  public static boolean isUuid(String input) {
    try {
      UUID.fromString(input);
      return true;
    } catch (IllegalArgumentException e) {
      return false;
    }
  }
}
