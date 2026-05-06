package com.vulinh.data.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.With;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
@With
public record TokenResult(
    AccountInfo user,
    @JsonProperty("access_token") String accessToken,
    @JsonProperty("token_type") String tokenType,
    @JsonProperty("expires_in") Long expiresIn,
    @JsonProperty("refresh_token") String refreshToken,
    @JsonProperty("refresh_token_expires_in") Long refreshTokenExpiresIn) {

  public static TokenResult full(
      AccountInfo user,
      String accessToken,
      long expiresIn,
      String refreshToken,
      long refreshTokenExpiresIn) {
    return new TokenResult(
        user, accessToken, "Bearer", expiresIn, refreshToken, refreshTokenExpiresIn);
  }

  public static TokenResult userOnly(AccountInfo user) {
    return new TokenResult(user, null, null, null, null, null);
  }
}
