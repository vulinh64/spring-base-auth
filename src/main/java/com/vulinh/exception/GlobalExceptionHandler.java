package com.vulinh.exception;

import com.vulinh.data.ServiceCodeError;
import com.vulinh.data.dto.GenericResponse;
import com.vulinh.locale.LocalizationSupport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends CommonExceptionHandler {

  // ApplicationValidationException → 400 is handled by CommonExceptionHandler since 3.0.0.

  /**
   * Override of the inherited 500 fallback: walks the cause chain so that {@link
   * ApplicationException}s thrown from DTO compact constructors (which Spring wraps in
   * {@code BeanInstantiationException} during data binding) still surface with their proper
   * error code, message, and status.
   */
  @Override
  @ExceptionHandler(RuntimeException.class)
  protected ResponseEntity<GenericResponse<Object>> handleRuntimeException(RuntimeException ex) {
    for (Throwable t = ex.getCause(); t != null; t = t.getCause()) {
      if (t instanceof ApplicationValidationException validation) {
        log.debug(validation.getMessage());
        return ResponseEntity.badRequest().body(GenericResponse.toError(validation));
      }
      if (t instanceof ApplicationException application) {
        log.error(application.getMessage(), application);
        return ResponseEntity.internalServerError().body(GenericResponse.toError(application));
      }
    }
    return super.handleRuntimeException(ex);
  }

  /** Account lookups that miss → 404. */
  @ExceptionHandler(AccountNotFoundException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public GenericResponse<Object> handleNotFoundException(AccountNotFoundException ex) {
    log.info(ex.getMessage());

    return GenericResponse.toError(ex);
  }

  /**
   * Service-to-AS authentication failure (missing or invalid {@code X-Service-Key}) → 401. The
   * {@link com.vulinh.configuration.SecurityConfig.ServiceApiKeyFilter ServiceApiKeyFilter}
   * routes its exceptions through Spring's {@code handlerExceptionResolver} so this handler
   * fires from the filter layer too.
   */
  @ExceptionHandler(ServiceAuthenticationException.class)
  @ResponseStatus(HttpStatus.UNAUTHORIZED)
  public GenericResponse<Object> handleServiceAuthenticationException(
      ServiceAuthenticationException ex) {
    log.info(ex.getMessage());

    return GenericResponse.toError(ex);
  }

  /**
   * JWT decode/signature/validator failures originating from controller-side decoding (e.g.
   * /refresh, /exchange). Security-chain JWT errors are handled by the resource-server filter and
   * never reach here.
   */
  @ExceptionHandler(JwtException.class)
  @ResponseStatus(HttpStatus.UNAUTHORIZED)
  public GenericResponse<Object> handleJwtException(JwtException ex) {
    log.debug(ex.getMessage());

    return GenericResponse.builder()
        .errorCode(ServiceCodeError.INVALID_TOKEN)
        .displayMessage(LocalizationSupport.getParsedMessage(ServiceCodeError.INVALID_TOKEN))
        .build();
  }
}
