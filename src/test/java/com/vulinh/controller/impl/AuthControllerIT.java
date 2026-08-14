package com.vulinh.controller.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import module java.base;

import com.vulinh.AuthIntegrationTestBase;
import com.vulinh.configuration.ApplicationProperties;
import com.vulinh.data.dto.LoginRequest;
import com.vulinh.data.dto.TokenType;
import com.vulinh.utils.JsonUtils;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

class AuthControllerIT extends AuthIntegrationTestBase {

  private static final String USER_ID = "00000000-0000-0000-0000-000000000020";
  private static final String ACCESS_TOKEN_COOKIE = "access_token";
  private static final String REFRESH_TOKEN_COOKIE = "refresh_token";
  private static final String CLIENT_ID = "spring-base";

  @Autowired private JwtDecoder jwtDecoder;

  @Autowired private ApplicationProperties applicationProperties;

  @Test
  void localProfileAllowsCorsRequestsFromAnyFrontendOrigin() throws Exception {
    var origin = "http://localhost:5173";

    mockMvc
        .perform(
            options("/auth/login")
                .header(HttpHeaders.ORIGIN, origin)
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
        .andExpect(status().isOk())
        .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin));
  }

  @Test
  void loginAndRefreshIssueCookieBasedTokenPairsForSeededUser() throws Exception {
    var loginResponse =
        mockMvc
            .perform(
                post("/auth/login")
                    .contentType(APPLICATION_JSON)
                    .with(csrf())
                    .content(
                        JsonUtils.toMinimizedJSON(
                            new LoginRequest("password", "spring-base", "user", "123456", null))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.user.id").value(USER_ID))
            .andExpect(jsonPath("$.data.user.username").value("user"))
            .andExpect(jsonPath("$.data.user.roles[0]").value("USER"))
            .andExpect(jsonPath("$.data.access_token").doesNotExist())
            .andExpect(jsonPath("$.data.refresh_token").doesNotExist())
            .andReturn()
            .getResponse();

    var loginAccessToken = loginResponse.getCookie(ACCESS_TOKEN_COOKIE);
    var refreshToken = loginResponse.getCookie(REFRESH_TOKEN_COOKIE);
    assertNotNull(loginAccessToken, "Login must return the access token as an HttpOnly cookie");
    assertNotNull(refreshToken, "Login must return the refresh token as an HttpOnly cookie");
    var decodedLoginAccessToken = jwtDecoder.decode(loginAccessToken.getValue());
    var decodedLoginRefreshToken = jwtDecoder.decode(refreshToken.getValue());
    assertTokenPair(decodedLoginAccessToken, decodedLoginRefreshToken);

    var refreshResponse =
        mockMvc
            .perform(
                post("/auth/refresh")
                    .with(csrf())
                    .cookie(new Cookie(REFRESH_TOKEN_COOKIE, refreshToken.getValue())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.user.id").value(USER_ID))
            .andExpect(jsonPath("$.data.user.username").value("user"))
            .andExpect(jsonPath("$.data.access_token").doesNotExist())
            .andExpect(jsonPath("$.data.refresh_token").doesNotExist())
            .andExpect(jsonPath("$.data.user.roles[0]").value("USER"))
            .andReturn()
            .getResponse();

    var refreshedAccessToken = refreshResponse.getCookie(ACCESS_TOKEN_COOKIE);
    var refreshedRefreshToken = refreshResponse.getCookie(REFRESH_TOKEN_COOKIE);
    assertNotNull(refreshedAccessToken, "Refresh must rotate the access token cookie");
    assertNotNull(refreshedRefreshToken, "Refresh must rotate the refresh token cookie");
    assertNotEquals(loginAccessToken.getValue(), refreshedAccessToken.getValue());
    assertNotEquals(refreshToken.getValue(), refreshedRefreshToken.getValue());

    assertTokenPair(
        jwtDecoder.decode(refreshedAccessToken.getValue()),
        jwtDecoder.decode(refreshedRefreshToken.getValue()));
  }

  private void assertTokenPair(Jwt accessToken, Jwt refreshToken) {
    assertCommonClaims(accessToken, TokenType.ACCESS, 1_800);
    assertEquals("user", accessToken.getClaimAsString("username"));
    assertEquals(List.of("USER"), accessToken.getClaimAsStringList("roles"));

    assertCommonClaims(refreshToken, TokenType.REFRESH, 86_400);
    assertFalse(refreshToken.hasClaim("username"));
    assertFalse(refreshToken.hasClaim("roles"));
  }

  private void assertCommonClaims(Jwt token, TokenType expectedType, long expectedLifetimeSeconds) {
    assertEquals("RS256", token.getHeaders().get("alg"));
    assertEquals(
        applicationProperties.security().issuerServer(), String.valueOf(token.getIssuer()));
    assertEquals(USER_ID, token.getSubject());
    assertEquals(List.of(CLIENT_ID), token.getAudience());
    assertEquals(CLIENT_ID, token.getClaimAsString(IdTokenClaimNames.AZP));
    assertEquals(expectedType.getTypeName(), token.getClaimAsString("typ"));
    assertNotNull(token.getId());
    assertNotNull(token.getIssuedAt());
    assertNotNull(token.getExpiresAt());
    assertEquals(
        expectedLifetimeSeconds,
        Duration.between(token.getIssuedAt(), token.getExpiresAt()).toSeconds());
  }
}
