package com.vulinh.configuration;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public interface InterServiceAuthenticator {

  String BE_TO_BE_ROLE = "SERVICE";

  default Authentication authenticate(HttpServletRequest request) {
    return new UsernamePasswordAuthenticationToken(
        "local-debug", null, List.of(new SimpleGrantedAuthority(BE_TO_BE_ROLE)));
  }
}
