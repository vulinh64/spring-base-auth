package com.vulinh.exception;

import module java.base;

import com.vulinh.data.ServiceCodeError;

/**
 * Thrown when an authenticated account holds no roles for the requested client (e.g. /me with a
 * token whose azp clientId no longer has any role-link to the account). Mapped to HTTP 403.
 */
public class ClientAccessDeniedException extends ApplicationException {

  @Serial private static final long serialVersionUID = 1L;

  public ClientAccessDeniedException(String message) {
    super(message, ServiceCodeError.NO_CLIENT_ACCESS);
  }
}
