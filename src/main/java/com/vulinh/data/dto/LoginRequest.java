package com.vulinh.data.dto;

import java.beans.ConstructorProperties;

public record LoginRequest(
    String grantType, String clientId, String username, String password, String otp) {

  @ConstructorProperties({"grant_type", "client_id", "username", "password", "otp"})
  public LoginRequest {}
}
