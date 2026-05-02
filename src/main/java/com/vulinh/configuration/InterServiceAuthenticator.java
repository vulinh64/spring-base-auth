package com.vulinh.configuration;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;

public interface InterServiceAuthenticator {

  String BE_TO_BE_ROLE = "SERVICE";

  Authentication authenticate(HttpServletRequest request);
}
