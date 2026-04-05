package com.vulinh.data.repository;

import com.vulinh.annotation.ExecutionTime;
import com.vulinh.data.entity.Account;
import com.vulinh.data.entity.AccountCredential.CredentialType;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AccountRepository extends JpaRepository<Account, UUID> {

  @Query(
      """
      select a from Account a
      join a.credentials c
      where a.username = :username
      and c.credentialType = :credentialType
      and c.enabled = true
      """)
  @EntityGraph(attributePaths = {"clientRoles", "credentials"})
  @ExecutionTime
  Optional<Account> fetchForLogin(String username, CredentialType credentialType);
}
