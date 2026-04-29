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
  INVALID_ACCOUNT("auth.invalid-account"),
  INVALID_TARGET_AUDIENCE("auth.invalid-target-audience"),
  UNSUPPORTED_GRANT_TYPE("auth.unsupported-grant-type"),
  MISSING_SESSION_TOKEN("auth.missing-session-token"),
  REFRESH_TOKEN_REQUIRED("auth.refresh-token-required"),
  AUDIENCE_REQUIRED("auth.audience-required"),
  GRANT_TYPE_REQUIRED("auth.grant-type-required"),
  CLIENT_ID_REQUIRED("auth.client-id-required"),
  MISSING_SERVICE_KEY("auth.missing-service-key"),
  INVALID_SERVICE_KEY("auth.invalid-service-key");

  private final String errorCode;
}
