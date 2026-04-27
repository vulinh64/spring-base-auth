package com.vulinh.configuration;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;

public class CookieBearerTokenResolver implements BearerTokenResolver {

  private final String cookieName;
  private final DefaultBearerTokenResolver defaultResolver = new DefaultBearerTokenResolver();

  public CookieBearerTokenResolver(String cookieName) {
    this.cookieName = cookieName;
  }

  @Override
  public String resolve(HttpServletRequest request) {
    var cookies = request.getCookies();

    if (cookies != null) {
      var token =
          Arrays.stream(cookies)
              .filter(cookie -> cookieName.equals(cookie.getName()))
              .map(Cookie::getValue)
              .findFirst()
              .orElse(null);

      if (token != null) {
        return token;
      }
    }

    return defaultResolver.resolve(request);
  }
}
