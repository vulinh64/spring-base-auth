package com.vulinh.data.repository;

import com.vulinh.data.entity.AccountCredential;
import com.vulinh.data.entity.AccountCredential.CredentialType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AccountCredentialRepository extends JpaRepository<AccountCredential, UUID> {

  @Query(
      """
      select ac from AccountCredential ac
      join fetch ac.account a
      where a.username = :username
        and ac.credentialType = :type
        and ac.enabled = true
      """)
  Optional<AccountCredential> findForLogin(String username, CredentialType type);
}
