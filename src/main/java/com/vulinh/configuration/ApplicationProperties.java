package com.vulinh.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "application-properties")
public record ApplicationProperties(Security security, Bootstrap bootstrap) {

  public record Security(TokenDelivery tokenDelivery, String issuerServer, String signingKey) {}

  public record Bootstrap(boolean peerDatabase) {}

  public enum TokenDelivery {
    COOKIE,
    HEADER,
    BODY
  }
}
