package com.vulinh.configuration;

import module java.base;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;

public class CookieBearerTokenResolver implements BearerTokenResolver {

  private static final DefaultBearerTokenResolver BEARER_TOKEN_RESOLVER =
      new DefaultBearerTokenResolver();

  private final String cookieName;

  public CookieBearerTokenResolver(String cookieName) {
    this.cookieName = cookieName;
  }

  @Override
  public String resolve(HttpServletRequest request) {
    var cookies = request.getCookies();

    return ArrayUtils.isNotEmpty(cookies)
        ? Arrays.stream(cookies)
            .filter(cookie -> cookieName.equals(cookie.getName()))
            .map(Cookie::getValue)
            .findFirst()
            .orElseGet(() -> BEARER_TOKEN_RESOLVER.resolve(request))
        : BEARER_TOKEN_RESOLVER.resolve(request);
  }
}
