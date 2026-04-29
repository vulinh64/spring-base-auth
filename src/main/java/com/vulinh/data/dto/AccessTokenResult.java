package com.vulinh.data.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AccessTokenResult(
    @JsonProperty("access_token") String accessToken,
    @JsonProperty("token_type") String tokenType,
    @JsonProperty("expires_in") long expiresIn,
    @JsonProperty("audience") String audience) {

  public AccessTokenResult(String accessToken, long expiresIn, String audience) {
    this(accessToken, "Bearer", expiresIn, audience);
  }
}
