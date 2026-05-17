package com.vulinh.exception;

import module java.base;

import com.vulinh.data.ServiceCodeError;

/**
 * Thrown post-authentication when the caller's account is disabled (refresh, /me). Mapped to HTTP
 * 401. Do not use in the login path — see {@link InvalidCredentialsException}.
 */
public class AccountDisabledException extends ApplicationException {

  @Serial private static final long serialVersionUID = 1L;

  public AccountDisabledException(String message) {
    super(message, ServiceCodeError.ACCOUNT_DISABLED);
  }
}
