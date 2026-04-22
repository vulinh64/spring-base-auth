package com.vulinh.controller.api;

import com.vulinh.data.dto.AccountBasicResponse;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/users")
public interface AccountAPI {

  @GetMapping("/{id}")
  AccountBasicResponse getAccountInfo(@PathVariable UUID id);
}
