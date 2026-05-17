package com.vulinh.configuration;

import module java.base;

import com.vulinh.data.event.EventType;
import lombok.Builder;
import lombok.With;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Builder
@With
@ConfigurationProperties(prefix = "application-properties")
public record ApplicationProperties(
    Security security, MessageTopic messageTopic, PeerDatabaseBootstrap peerDatabaseBootstrap) {

  @Builder
  @With
  public record MessageTopic(TopicProperties keyInvalidated) {}

  @Builder
  @With
  public record TopicProperties(EventType type, String topicName) {}

  @Builder
  @With
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

  @Builder
  @With
  public record PeerDatabaseBootstrap(boolean enabled, List<PeerDatabase> peerDatabases) {

    @Builder
    @With
    public record PeerDatabase(String databaseName, String user) {}
  }
}
