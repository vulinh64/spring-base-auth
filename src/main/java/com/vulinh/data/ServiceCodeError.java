package com.vulinh.data;

import com.vulinh.utils.validator.ApplicationError;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ServiceCodeError implements ApplicationError {
  ACCOUNT_NOT_FOUND("accounts.account-not-found");

  private final String errorCode;
}
