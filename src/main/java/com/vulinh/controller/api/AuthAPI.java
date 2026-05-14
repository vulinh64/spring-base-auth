package com.vulinh.controller.api;

import com.vulinh.data.dto.LoginRequest;
import com.vulinh.data.dto.TokenResult;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

@RequestMapping("/auth")
public interface AuthAPI {

  @PostMapping("/login")
  TokenResult login(@RequestBody LoginRequest request, HttpServletResponse response);

  @PostMapping("/refresh")
  TokenResult refresh(HttpServletRequest request, HttpServletResponse response);

  @PostMapping("/logout")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void logout(HttpServletResponse response);
}
