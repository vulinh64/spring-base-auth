package com.vulinh.data.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TokenResult(
    @JsonProperty("session_token") String sessionToken,
    @JsonProperty("token_type") String tokenType,
    @JsonProperty("expires_in") long expiresIn,
    @JsonProperty("refresh_token") String refreshToken,
    @JsonProperty("refresh_token_expires_in") long refreshTokenExpiresIn) {

  public TokenResult(
      String sessionToken, long expiresIn, String refreshToken, long refreshTokenExpiresIn) {
    this(sessionToken, "Bearer", expiresIn, refreshToken, refreshTokenExpiresIn);
  }
}
