package com.vulinh.configuration;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public interface InterServiceAuthenticator {

  String BE_TO_BE_ROLE = "SERVICE";

  Authentication authenticate(HttpServletRequest request);
}
