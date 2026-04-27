package com.vulinh.controller.impl;

import com.vulinh.configuration.ApplicationProperties;
import com.vulinh.controller.api.AuthAPI;
import com.vulinh.data.dto.LoginRequest;
import com.vulinh.data.dto.RefreshRequest;
import com.vulinh.data.dto.TokenResult;
import com.vulinh.service.AuthService;
import jakarta.servlet.http.Cookie;
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
    return deliverTokens(authService.login(request), response);
  }

  @Override
  public TokenResult refresh(RefreshRequest request, HttpServletResponse response) {
    return deliverTokens(authService.refresh(request), response);
  }

  private TokenResult deliverTokens(TokenResult result, HttpServletResponse response) {
    var security = applicationProperties.security();
    return switch (security.tokenDelivery()) {
      case COOKIE -> {
        response.addCookie(
            createCookie(
                security.accessTokenCookieName(),
                result.accessToken(),
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
        response.setHeader("X-Access-Token", result.accessToken());
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
}
