package com.vulinh.controller.impl;

import com.vulinh.configuration.ApplicationProperties;
import com.vulinh.controller.api.AuthAPI;
import com.vulinh.data.ServiceCodeError;
import com.vulinh.data.dto.GenericResponse;
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
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.core.OAuth2AccessToken.TokenType;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AuthController implements AuthAPI {

  static final String X_ACCESS_TOKEN_HEADER = "X-Access-Token";
  static final String X_REFRESH_TOKEN_HEADER = "X-Refresh-Token";

  private static final String BEARER_PREFIX = TokenType.BEARER + " ";

  private final AuthService authService;
  private final ApplicationProperties applicationProperties;

  @Override
  public GenericResponse<TokenResult> login(LoginRequest request, HttpServletResponse response) {
    requireNonBlank(request.grantType(), ServiceCodeError.GRANT_TYPE_REQUIRED);
    requireNonBlank(request.clientId(), ServiceCodeError.CLIENT_ID_REQUIRED);

    return GenericResponse.success(deliverTokenPair(authService.login(request), response));
  }

  @Override
  public GenericResponse<TokenResult> refresh(
      HttpServletRequest request, HttpServletResponse response) {
    var refreshToken = resolveRefreshToken(request);

    requireNonBlank(refreshToken, ServiceCodeError.REFRESH_TOKEN_REQUIRED);

    return GenericResponse.success(
        deliverTokenPair(authService.refresh(new RefreshRequest(refreshToken)), response));
  }

  private String resolveRefreshToken(HttpServletRequest request) {
    var cookieName = applicationProperties.security().refreshTokenCookieName();
    var cookies = request.getCookies();

    if (cookies != null) {
      for (var cookie : cookies) {
        if (cookieName.equals(cookie.getName()) && StringUtils.isNotBlank(cookie.getValue())) {
          return cookie.getValue();
        }
      }
    }

    var authorization = request.getHeader(HttpHeaders.AUTHORIZATION);

    if (Strings.CS.startsWith(authorization, BEARER_PREFIX)) {
      return authorization.substring(BEARER_PREFIX.length()).trim();
    }

    return null;
  }

  @Override
  public void logout(HttpServletResponse response) {
    var security = applicationProperties.security();
    switch (security.tokenDelivery()) {
      case COOKIE -> {
        response.addCookie(
            createCookie(
                security.accessTokenCookieName(), StringUtils.EMPTY, 0, security.cookieSecure()));
        response.addCookie(
            createCookie(
                security.refreshTokenCookieName(), StringUtils.EMPTY, 0, security.cookieSecure()));
      }
      case HEADER -> {
        response.setHeader(X_ACCESS_TOKEN_HEADER, StringUtils.EMPTY);
        response.setHeader(X_REFRESH_TOKEN_HEADER, StringUtils.EMPTY);
      }
      case BODY -> {
        // No-op: BODY-mode clients hold tokens themselves and discard client-side.
      }
    }
  }

  private TokenResult deliverTokenPair(TokenResult result, HttpServletResponse response) {
    var security = applicationProperties.security();

    var accessToken = result.accessToken();
    var refreshToken = result.refreshToken();

    return switch (security.tokenDelivery()) {
      case COOKIE -> {
        var isSecure = security.cookieSecure();

        response.addCookie(
            createCookie(
                security.accessTokenCookieName(), accessToken, result.expiresIn(), isSecure));

        response.addCookie(
            createCookie(
                security.refreshTokenCookieName(),
                refreshToken,
                result.refreshTokenExpiresIn(),
                isSecure));

        yield TokenResult.userOnly(result.user());
      }
      case HEADER -> {
        response.setHeader(X_ACCESS_TOKEN_HEADER, accessToken);
        response.setHeader(X_REFRESH_TOKEN_HEADER, refreshToken);

        yield TokenResult.userOnly(result.user());
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
          "%s is missing or blank".formatted(error.getErrorCode()), error);
    }
  }
}
