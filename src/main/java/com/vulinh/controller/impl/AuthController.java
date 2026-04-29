package com.vulinh.controller.impl;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

import com.vulinh.configuration.ApplicationProperties;
import com.vulinh.controller.api.AuthAPI;
import com.vulinh.data.ServiceCodeError;
import com.vulinh.data.dto.AccessTokenResult;
import com.vulinh.data.dto.ExchangeRequest;
import com.vulinh.data.dto.LoginRequest;
import com.vulinh.data.dto.RefreshRequest;
import com.vulinh.data.dto.TokenResult;
import com.vulinh.exception.ApplicationValidationException;
import com.vulinh.service.AuthService;
import com.vulinh.utils.validator.ApplicationError;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AuthController implements AuthAPI {

  private final AuthService authService;
  private final ApplicationProperties applicationProperties;

  @Override
  public TokenResult login(LoginRequest request, HttpServletResponse response) {
    requireNonBlank(request.grantType(), ServiceCodeError.GRANT_TYPE_REQUIRED);
    requireNonBlank(request.clientId(), ServiceCodeError.CLIENT_ID_REQUIRED);
    return deliverSessionPair(authService.login(request), response);
  }

  @Override
  public TokenResult refresh(RefreshRequest request, HttpServletResponse response) {
    requireNonBlank(request.refreshToken(), ServiceCodeError.REFRESH_TOKEN_REQUIRED);
    return deliverSessionPair(authService.refresh(request), response);
  }

  @Override
  public AccessTokenResult exchange(ExchangeRequest request, HttpServletRequest httpRequest) {
    requireNonBlank(request.audience(), ServiceCodeError.AUDIENCE_REQUIRED);

    var sessionToken = readSessionToken(httpRequest);
    if (sessionToken == null) {
      throw new ApplicationValidationException(
          "Missing session_token cookie/header on /exchange",
          ServiceCodeError.MISSING_SESSION_TOKEN);
    }
    // Access tokens are always returned in the body — they are short-lived,
    // audience-scoped, and consumed by the FE choosing which BE to call.
    return authService.exchange(sessionToken, request.audience());
  }

  private String readSessionToken(HttpServletRequest request) {
    var cookieName = applicationProperties.security().sessionTokenCookieName();
    if (request.getCookies() != null) {
      for (var cookie : request.getCookies()) {
        if (cookieName.equals(cookie.getName())) {
          return cookie.getValue();
        }
      }
    }
    var header = request.getHeader(AUTHORIZATION);
    if (header != null && header.startsWith("Bearer ")) {
      return header.substring(7);
    }
    return null;
  }

  private TokenResult deliverSessionPair(TokenResult result, HttpServletResponse response) {
    var security = applicationProperties.security();
    return switch (security.tokenDelivery()) {
      case COOKIE -> {
        response.addCookie(
            createCookie(
                security.sessionTokenCookieName(),
                result.sessionToken(),
                result.expiresIn(),
                security.cookieSecure()));
        response.addCookie(
            createCookie(
                security.refreshTokenCookieName(),
                result.refreshToken(),
                result.refreshTokenExpiresIn(),
                security.cookieSecure()));
        yield null;
      }
      case HEADER -> {
        response.setHeader("X-Session-Token", result.sessionToken());
        response.setHeader("X-Refresh-Token", result.refreshToken());
        yield null;
      }
      case BODY -> result;
    };
  }

  private static Cookie createCookie(String name, String value, long maxAge, boolean secure) {
    var cookie = new Cookie(name, value);
    cookie.setMaxAge(Math.toIntExact(maxAge));
    cookie.setSecure(secure);
    cookie.setHttpOnly(true);
    cookie.setPath("/");
    return cookie;
  }

  private static void requireNonBlank(String value, ApplicationError error) {
    if (value == null || value.isBlank()) {
      throw new ApplicationValidationException(
          error.getErrorCode() + " is missing or blank", error);
    }
  }
}
