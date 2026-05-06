package com.vulinh.configuration;

import module java.base;

import com.vulinh.data.event.EventType;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "application-properties")
public record ApplicationProperties(
    Security security, MessageTopic messageTopic, PeerDatabaseBootstrap peerDatabaseBootstrap) {

  public record MessageTopic(TopicProperties keyInvalidated) {}

  public record TopicProperties(EventType type, String topicName) {}

  @SuppressWarnings("java:S6218")
  public record Security(
      TokenDelivery tokenDelivery,
      String issuerServer,
      String jwksPath,
      String discoveryPath,
      String[] noAuthUrls,
      String accessTokenCookieName,
      String refreshTokenCookieName,
      boolean cookieSecure) {

    public enum TokenDelivery {
      COOKIE,
      HEADER,
      BODY
    }
  }

  public record PeerDatabaseBootstrap(boolean enabled, List<String> peerDatabases) {}
}
