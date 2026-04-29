package com.vulinh.data.repository;

import com.vulinh.data.entity.Client;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;

public interface ClientRepository
    extends JpaRepository<Client, UUID>, QuerydslPredicateExecutor<Client> {

  Optional<Client> findByClientIdAndEnabledIsTrue(String clientId);

  Optional<Client> findByServiceApiKeyHashAndEnabledIsTrue(String serviceApiKeyHash);
}
