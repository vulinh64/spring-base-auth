package com.vulinh.data.repository;

import com.vulinh.data.entity.Account;
import com.vulinh.data.entity.AccountCredential.CredentialType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccountRepositoryCustom {

  Optional<Account> fetchForLogin(String username, CredentialType credentialType);

  List<String> findRoleNames(UUID accountId, UUID clientId);
}
