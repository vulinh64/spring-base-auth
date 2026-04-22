package com.vulinh.service.user;

import com.vulinh.data.dto.AccountBasicResponse;
import com.vulinh.data.mapper.AccountMapper;
import com.vulinh.data.repository.AccountRepository;
import com.vulinh.exception.AccountNotFoundException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccountService {

  final AccountRepository accountRepository;

  public AccountBasicResponse getUserInfo(UUID id) {
    return accountRepository
        .findById(id)
        .map(AccountMapper.INSTANCE::toBasicResponse)
        .orElseThrow(() -> new AccountNotFoundException("User ID [%s] not found".formatted(id)));
  }
}
