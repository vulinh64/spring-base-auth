package com.vulinh.controller.api;

import module java.base;

import com.vulinh.data.dto.response.AccountBasicResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/internal/users")
public interface InternalUserAPI {

  /** BE-to-AS lookup. Gated by X-Service-Key (or bypassed in local profile). */
  @GetMapping("/{id}")
  AccountBasicResponse getUser(@PathVariable UUID id);
}
