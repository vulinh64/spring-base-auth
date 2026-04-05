package com.vulinh.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "application-properties")
public record ApplicationProperties(Security security) {

  public record Security(TokenDelivery tokenDelivery, String issuerServer) {}

  public enum TokenDelivery {
    COOKIE,
    HEADER,
    BODY
  }
}
