package com.vulinh.controller.impl;

import com.vulinh.controller.api.AccountAPI;
import com.vulinh.data.dto.AccountBasicResponse;
import com.vulinh.service.user.AccountService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AccountController implements AccountAPI {

  private final AccountService accountService;

  @Override
  public AccountBasicResponse getAccountInfo(UUID id) {
    return accountService.getUserInfo(id);
  }
}
