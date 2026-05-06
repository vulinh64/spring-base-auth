package com.vulinh.exception;

import module java.base;

import com.vulinh.data.ServiceCodeError;

/** Thrown when a referenced client_id is unknown or disabled. Mapped to HTTP 400. */
public class ClientNotFoundException extends ApplicationException {

  @Serial private static final long serialVersionUID = 1L;

  public ClientNotFoundException(String message) {
    super(message, ServiceCodeError.INVALID_CLIENT);
  }
}
