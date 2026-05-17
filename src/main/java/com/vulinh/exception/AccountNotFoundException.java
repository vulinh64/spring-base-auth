package com.vulinh.exception;

import module java.base;

import com.vulinh.data.ServiceCodeError;

public class AccountNotFoundException extends ApplicationException {

  @Serial private static final long serialVersionUID = 1L;

  public AccountNotFoundException(String message) {
    super(message, ServiceCodeError.ACCOUNT_NOT_FOUND);
  }
}
