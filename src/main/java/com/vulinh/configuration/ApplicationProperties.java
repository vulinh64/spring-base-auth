package com.vulinh.configuration;

import module java.base;

import com.vulinh.data.config.HttpMethodUrl;
import com.vulinh.data.config.RecordPublicSecurityPath;
import com.vulinh.data.event.EventType;
import com.vulinh.utils.CollectionHelper;
import lombok.Builder;
import lombok.With;
import org.apache.commons.lang3.StringUtils;
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
      List<String> noAuthUrls,
      List<HttpMethodUrl> noAuthMethodUrls,
      List<String> corsAllowedOrigins,
      String accessTokenCookieName,
      String refreshTokenCookieName,
      boolean cookieSecure)
      implements RecordPublicSecurityPath {

    public Security {
      noAuthUrls = CollectionHelper.emptyListIfNull(noAuthUrls);
      noAuthMethodUrls = CollectionHelper.emptyListIfNull(noAuthMethodUrls);
      corsAllowedOrigins =
          CollectionHelper.emptyListIfNull(corsAllowedOrigins).stream()
              .filter(StringUtils::isNotBlank)
              .toList();
    }

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
