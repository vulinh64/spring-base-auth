package com.vulinh.controller.api;

import com.vulinh.data.dto.AccountInfo;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/accounts")
public interface AccountAPI {

  @GetMapping("/me")
  AccountInfo getOwnAccount(@AuthenticationPrincipal Jwt jwt);
}
