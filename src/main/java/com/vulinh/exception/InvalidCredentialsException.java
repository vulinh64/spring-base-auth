package com.vulinh.exception;

import module java.base;

import com.vulinh.data.ServiceCodeError;

/**
 * Thrown by credential strategies on any login-time check failure (wrong password, wrong/expired
 * OTP, disabled account during login). Mapped to HTTP 401. The error message is intentionally
 * uniform on the client side — distinguishing failure modes leaks account-existence information.
 */
public class InvalidCredentialsException extends ApplicationException {

  @Serial private static final long serialVersionUID = 1L;

  public InvalidCredentialsException(String message) {
    super(message, ServiceCodeError.INVALID_CREDENTIALS);
  }
}
