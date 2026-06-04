package com.vulinh.data.predicate;

import module java.base;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.vulinh.data.entity.QClient;

public final class ClientPredicate {

  private final BooleanBuilder predicate;

  public ClientPredicate() {
    predicate = new BooleanBuilder(QClient.client.enabled.eq(true));
  }

  public ClientPredicate byClientId(String clientId) {
    var c = QClient.client;

    predicate.and(isUuid(clientId) ? c.id.eq(UUID.fromString(clientId)) : c.clientId.eq(clientId));

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
    } catch (IllegalArgumentException _) {
      return false;
    }
  }
}
