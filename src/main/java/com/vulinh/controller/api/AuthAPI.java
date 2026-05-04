package com.vulinh.controller.api;

import com.vulinh.data.dto.AccessTokenResult;
import com.vulinh.data.dto.ExchangeRequest;
import com.vulinh.data.dto.LoginRequest;
import com.vulinh.data.dto.RefreshRequest;
import com.vulinh.data.dto.TokenResult;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/auth")
public interface AuthAPI {

  @PostMapping("/login")
  TokenResult login(@RequestBody LoginRequest request, HttpServletResponse response);

  @PostMapping("/refresh")
  TokenResult refresh(@RequestBody RefreshRequest request, HttpServletResponse response);

  @PostMapping("/exchange")
  AccessTokenResult exchange(@RequestBody ExchangeRequest request, HttpServletRequest httpRequest);
}
