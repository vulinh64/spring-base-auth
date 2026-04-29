package com.vulinh.configuration;

import com.vulinh.data.dto.TokenType;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

public record JwtTypValidator(TokenType expectedType) implements OAuth2TokenValidator<Jwt> {

  @Override
  public OAuth2TokenValidatorResult validate(Jwt jwt) {
    if (expectedType.getTypeName().equals(jwt.getClaimAsString("typ"))) {
      return OAuth2TokenValidatorResult.success();
    }
    return OAuth2TokenValidatorResult.failure(
        new OAuth2Error(
            "invalid_token",
            "Required token type '%s' is missing".formatted(expectedType.getTypeName()),
            null));
  }
}
