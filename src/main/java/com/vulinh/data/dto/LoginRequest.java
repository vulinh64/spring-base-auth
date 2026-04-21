package com.vulinh.data.dto;

import java.beans.ConstructorProperties;
import java.util.UUID;

public record LoginRequest(
    String grantType, UUID clientId, String username, String password, String otp) {

  @ConstructorProperties({"grant_type", "client_id", "username", "password", "otp"})
  public LoginRequest {
    // Compact constructor does nothing
  }
}
