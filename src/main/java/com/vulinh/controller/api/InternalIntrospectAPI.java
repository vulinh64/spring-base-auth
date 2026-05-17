package com.vulinh.controller.api;

import com.vulinh.data.dto.IntrospectRequest;
import com.vulinh.data.dto.IntrospectResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/internal/introspect")
public interface InternalIntrospectAPI {

  /**
   * Decodes a JWT and returns its claims (or {@code active:false} with a reason if the token
   * fails verification). Intended as a debug helper for BE service authors integrating against
   * this auth server. Gated by the same {@code X-Service-Key} as other {@code /internal/**}
   * endpoints (or bypassed in {@code local} profile).
   */
  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  IntrospectResponse introspect(@RequestBody IntrospectRequest request);
}
