package com.vulinh.data.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.vulinh.data.entity.Account;
import com.vulinh.data.entity.AccountCredential.CredentialType;
import com.vulinh.data.entity.QAccount;
import com.vulinh.data.entity.QAccountCredential;
import com.vulinh.data.entity.QClientRole;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class AccountRepositoryImpl implements AccountRepositoryCustom {

  private final JPAQueryFactory queryFactory;

  public AccountRepositoryImpl(EntityManager entityManager) {
    this.queryFactory = new JPAQueryFactory(entityManager);
  }

  @Override
  public Optional<Account> fetchForLogin(String username, CredentialType credentialType) {
    var account = QAccount.account;
    var credential = QAccountCredential.accountCredential;

    return Optional.ofNullable(
        queryFactory
            .selectFrom(account)
            .join(account.credentials, credential)
            .fetchJoin()
            .where(
                account.username.eq(username),
                credential.credentialType.eq(credentialType),
                credential.enabled.isTrue())
            .fetchOne());
  }

  @Override
  public List<String> findRoleNames(UUID accountId, UUID clientId) {
    var account = QAccount.account;
    var clientRole = QClientRole.clientRole;

    return queryFactory
        .select(clientRole.roleName)
        .from(account)
        .join(account.clientRoles, clientRole)
        .where(account.id.eq(accountId), clientRole.clientId.eq(clientId))
        .fetch();
  }
}
