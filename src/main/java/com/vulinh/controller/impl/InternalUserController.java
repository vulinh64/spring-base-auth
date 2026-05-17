package com.vulinh.controller.impl;

import module java.base;

import com.vulinh.controller.api.InternalUserAPI;
import com.vulinh.data.dto.response.AccountBasicResponse;
import com.vulinh.service.user.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class InternalUserController implements InternalUserAPI {

  private final AccountService accountService;

  @Override
  public AccountBasicResponse getUser(UUID id) {
    return accountService.getAccountInfo(id);
  }
}
