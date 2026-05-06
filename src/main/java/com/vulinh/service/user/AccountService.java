package com.vulinh.service.user;

import module java.base;

import com.vulinh.data.dto.AccountInfo;
import com.vulinh.data.dto.response.AccountBasicResponse;
import com.vulinh.data.entity.Account;
import com.vulinh.data.mapper.AccountMapper;
import com.vulinh.data.repository.AccountRepository;
import com.vulinh.exception.AccountDisabledException;
import com.vulinh.exception.AccountNotFoundException;
import com.vulinh.exception.ClientAccessDeniedException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccountService {

  final AccountRepository accountRepository;

  public AccountBasicResponse getAccountInfo(UUID id) {
    return accountRepository
        .findById(id)
        .map(AccountMapper.INSTANCE::toBasicResponse)
        .orElseThrow(() -> new AccountNotFoundException("Account ID [%s] not found".formatted(id)));
  }

  public AccountInfo getOwnAccount(UUID accountId, String clientId) {
    var account =
        accountRepository
            .findById(accountId)
            .filter(Account::isAccountEnabled)
            .orElseThrow(
                () -> new AccountDisabledException("Account [%s] not active".formatted(accountId)));

    var roles = accountRepository.findRoleNames(accountId, clientId);

    if (roles.isEmpty()) {
      throw new ClientAccessDeniedException(
          "Account [%s] has no roles for client [%s]".formatted(accountId, clientId));
    }

    return AccountInfo.from(account, roles);
  }
}
