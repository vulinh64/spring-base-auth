package com.vulinh.controller.impl;

import com.vulinh.controller.api.InternalIntrospectAPI;
import com.vulinh.data.dto.IntrospectRequest;
import com.vulinh.data.dto.IntrospectResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class InternalIntrospectController implements InternalIntrospectAPI {

  private final JwtDecoder jwtDecoder;

  @Override
  public IntrospectResponse introspect(IntrospectRequest request) {
    if (request.token() == null || request.token().isBlank()) {
      return IntrospectResponse.invalid("token is missing");
    }
    try {
      var jwt = jwtDecoder.decode(request.token());
      return IntrospectResponse.valid(jwt.getClaims());
    } catch (JwtException ex) {
      // Use getClass().getSimpleName() to keep the response stable across Nimbus version changes.
      return IntrospectResponse.invalid(ex.getMessage());
    }
  }
}
