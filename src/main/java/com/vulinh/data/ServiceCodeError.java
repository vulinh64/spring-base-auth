package com.vulinh.data;

import com.vulinh.utils.validator.ApplicationError;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ServiceCodeError implements ApplicationError {
  ACCOUNT_NOT_FOUND("accounts.account-not-found"),
  INVALID_CLIENT("auth.invalid-client"),
  INVALID_TOKEN("auth.invalid-token"),
  INVALID_TOKEN_TYPE("auth.invalid-token-type"),
  INVALID_AUDIENCE("auth.invalid-audience"),
  INVALID_CREDENTIALS("auth.invalid-credentials"),
  ACCOUNT_DISABLED("auth.account-disabled"),
  NO_CLIENT_ACCESS("auth.no-client-access"),
  UNSUPPORTED_GRANT_TYPE("auth.unsupported-grant-type"),
  REFRESH_TOKEN_REQUIRED("auth.refresh-token-required"),
  GRANT_TYPE_REQUIRED("auth.grant-type-required"),
  CLIENT_ID_REQUIRED("auth.client-id-required"),
  MISSING_SERVICE_KEY("auth.missing-service-key"),
  INVALID_SERVICE_KEY("auth.invalid-service-key");

  private final String errorCode;
}
