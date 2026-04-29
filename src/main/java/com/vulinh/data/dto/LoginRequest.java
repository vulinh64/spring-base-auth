package com.vulinh.data.dto;

public record LoginRequest(
    String grantType, String clientId, String username, String password, String otp) {}
