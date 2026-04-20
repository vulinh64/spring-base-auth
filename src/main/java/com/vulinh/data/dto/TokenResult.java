package com.vulinh.data.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TokenResult(
    @JsonProperty("access_token") String accessToken,
    @JsonProperty("token_type") String tokenType,
    @JsonProperty("expires_in") long expiresIn,
    @JsonProperty("refresh_token") String refreshToken,
    @JsonProperty("refresh_token_expires_in") long refreshTokenExpiresIn) {

  public TokenResult(
      String accessToken, long expiresIn, String refreshToken, long refreshTokenExpiresIn) {
    this(accessToken, "Bearer", expiresIn, refreshToken, refreshTokenExpiresIn);
  }
}
