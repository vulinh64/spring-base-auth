package com.vulinh.exception;

import module java.base;

import com.vulinh.data.ServiceCodeError;

/**
 * Thrown by manual JWT inspection (e.g. {@code typ} mismatch on /refresh). Mapped to HTTP 401.
 * Signature/issuer/expiry failures from Spring's resource server come through {@link
 * org.springframework.security.oauth2.jwt.JwtException} instead.
 */
public class InvalidTokenException extends ApplicationException {

  @Serial private static final long serialVersionUID = 1L;

  public InvalidTokenException(String message) {
    super(message, ServiceCodeError.INVALID_TOKEN_TYPE);
  }
}
