package com.vulinh.controller.impl;

import module java.base;

import com.vulinh.controller.api.AccountAPI;
import com.vulinh.data.dto.AccountInfo;
import com.vulinh.data.dto.GenericResponse;
import com.vulinh.service.user.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AccountController implements AccountAPI {

  private final AccountService accountService;

  @Override
  public GenericResponse<AccountInfo> getOwnAccount(Jwt jwt) {
    return GenericResponse.success(
        accountService.getOwnAccount(
            UUID.fromString(jwt.getSubject()), jwt.getClaimAsString(IdTokenClaimNames.AZP)));
  }
}
