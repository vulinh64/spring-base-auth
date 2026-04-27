package com.vulinh.controller.api;

import module java.base;

import com.vulinh.data.dto.response.AccountBasicResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/accounts")
public interface AccountAPI {

  @GetMapping("/{id}")
  AccountBasicResponse getAccountInfo(@PathVariable UUID id);
}
