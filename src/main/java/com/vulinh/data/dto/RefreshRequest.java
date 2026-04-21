package com.vulinh.data.dto;

import java.beans.ConstructorProperties;

public record RefreshRequest(String refreshToken) {

  @ConstructorProperties("refresh_token")
  public RefreshRequest {
    // Compact constructor does nothing
  }
}
