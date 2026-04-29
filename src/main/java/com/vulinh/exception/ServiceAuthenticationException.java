package com.vulinh.exception;

import com.vulinh.utils.validator.ApplicationError;
import java.io.Serial;

/**
 * Thrown when a request to {@code /internal/**} cannot be authenticated as a known service —
 * either the {@code X-Service-Key} header is missing, or the presented key does not match any
 * registered client. Mapped to HTTP 401 by {@link GlobalExceptionHandler}.
 */
public class ServiceAuthenticationException extends ApplicationException {

  @Serial private static final long serialVersionUID = 1L;

  public ServiceAuthenticationException(String message, ApplicationError error, Object... args) {
    super(message, error, args);
  }
}
