package com.vulinh.controller;

import com.vulinh.data.dto.LoginRequest;
import com.vulinh.data.dto.RefreshRequest;
import com.vulinh.data.dto.TokenResult;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/auth")
public interface AuthAPI {

  @PostMapping(value = "/login", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
  TokenResult login(LoginRequest request, HttpServletResponse response);

  @PostMapping(value = "/refresh", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
  TokenResult refresh(RefreshRequest request, HttpServletResponse response);
}
