package com.vulinh.controller.api;

import com.vulinh.data.dto.AccessTokenResult;
import com.vulinh.data.dto.ExchangeRequest;
import com.vulinh.data.dto.LoginRequest;
import com.vulinh.data.dto.RefreshRequest;
import com.vulinh.data.dto.TokenResult;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping
public interface AuthAPI {

  @PostMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE)
  TokenResult login(@RequestBody LoginRequest request, HttpServletResponse response);

  @PostMapping(value = "/refresh", consumes = MediaType.APPLICATION_JSON_VALUE)
  TokenResult refresh(@RequestBody RefreshRequest request, HttpServletResponse response);

  @PostMapping(value = "/exchange", consumes = MediaType.APPLICATION_JSON_VALUE)
  AccessTokenResult exchange(@RequestBody ExchangeRequest request, HttpServletRequest httpRequest);
}
