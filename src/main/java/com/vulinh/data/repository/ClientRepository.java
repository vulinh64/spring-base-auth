package com.vulinh.data.repository;

import module java.base;

import com.vulinh.data.entity.Client;
import com.vulinh.data.predicate.ClientPredicate;

public interface ClientRepository extends BaseRepository<Client, UUID> {

  default Optional<Client> findByClientIdAndEnabledIsTrue(String clientId) {
    return findOne(new ClientPredicate().byClientId(clientId).toPredicate());
  }
}
