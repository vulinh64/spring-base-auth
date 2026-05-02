package com.vulinh.configuration;

import module java.base;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

@Component
@Profile("local")
public class LocalDebugInterServiceAuthenticator implements InterServiceAuthenticator {

}
