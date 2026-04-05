package com.vulinh.data.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TokenResult(
    @JsonProperty("access_token") String accessToken,
    @JsonProperty("token_type") String tokenType,
    @JsonProperty("expires_in") int expiresIn,
    @JsonProperty("refresh_token") String refreshToken,
    @JsonProperty("refresh_token_expires_in") int refreshTokenExpiresIn) {

  public TokenResult(String accessToken, int expiresIn, String refreshToken, int refreshTokenExpiresIn) {
    this(accessToken, "Bearer", expiresIn, refreshToken, refreshTokenExpiresIn);
  }
}
