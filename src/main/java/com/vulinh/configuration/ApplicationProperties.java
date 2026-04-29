package com.vulinh.configuration;

import com.vulinh.data.event.EventType;
import java.util.Arrays;
import java.util.Objects;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "application-properties")
public record ApplicationProperties(
    Security security, MessageTopic messageTopic, Bootstrap bootstrap) {

  public record MessageTopic(TopicProperties keyInvalidated) {}

  public record TopicProperties(EventType type, String topicName) {}

  public record Security(
      TokenDelivery tokenDelivery,
      String issuerServer,
      String jwksPath,
      String discoveryPath,
      String[] noAuthUrls,
      String sessionTokenCookieName,
      String refreshTokenCookieName,
      boolean cookieSecure,
      boolean skipServiceKeyVerification) {

    public enum TokenDelivery {
      COOKIE,
      HEADER,
      BODY
    }

    @Override
    public boolean equals(Object other) {
      if (other
          instanceof
          Security(
              TokenDelivery delivery,
              String server,
              String jwks,
              String discovery,
              String[] authUrls,
              String sessionCookie,
              String refreshCookie,
              boolean secure,
              boolean skipKey)) {
        return Objects.equals(issuerServer, server)
            && Objects.equals(jwksPath, jwks)
            && Objects.equals(discoveryPath, discovery)
            && Objects.deepEquals(noAuthUrls, authUrls)
            && Objects.equals(sessionTokenCookieName, sessionCookie)
            && Objects.equals(refreshTokenCookieName, refreshCookie)
            && cookieSecure == secure
            && skipServiceKeyVerification == skipKey
            && tokenDelivery == delivery;
      }
      return false;
    }

    @Override
    public int hashCode() {
      return Objects.hash(
          tokenDelivery,
          issuerServer,
          jwksPath,
          discoveryPath,
          Arrays.hashCode(noAuthUrls),
          sessionTokenCookieName,
          refreshTokenCookieName,
          cookieSecure,
          skipServiceKeyVerification);
    }

    @Override
    @NonNull
    public String toString() {
      return ("Security{tokenDelivery=%s, issuerServer='%s', jwksPath='%s', discoveryPath='%s', "
              + "noAuthUrls=%s, sessionTokenCookieName='%s', refreshTokenCookieName='%s', "
              + "cookieSecure=%s, skipServiceKeyVerification=%s}")
          .formatted(
              tokenDelivery,
              issuerServer,
              jwksPath,
              discoveryPath,
              Arrays.toString(noAuthUrls),
              sessionTokenCookieName,
              refreshTokenCookieName,
              cookieSecure,
              skipServiceKeyVerification);
    }
  }

  public record Bootstrap(boolean peerDatabase) {}

  // -- end of nested types --
}
