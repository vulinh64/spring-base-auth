package com.vulinh.configuration;

import java.util.Arrays;
import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.lang.NonNull;

@ConfigurationProperties(prefix = "application-properties")
public record ApplicationProperties(Security security, Bootstrap bootstrap) {

  public record Security(
      TokenDelivery tokenDelivery,
      String issuerServer,
      String[] noAuthUrls,
      String accessTokenCookieName,
      String refreshTokenCookieName,
      boolean cookieSecure) {

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
              String[] authUrls,
              String accessCookie,
              String refreshCookie,
              boolean secure)) {
        return Objects.equals(issuerServer, server)
            && Objects.deepEquals(noAuthUrls, authUrls)
            && Objects.equals(accessTokenCookieName, accessCookie)
            && Objects.equals(refreshTokenCookieName, refreshCookie)
            && cookieSecure == secure
            && tokenDelivery == delivery;
      }
      return false;
    }

    @Override
    public int hashCode() {
      return Objects.hash(
          tokenDelivery,
          issuerServer,
          Arrays.hashCode(noAuthUrls),
          accessTokenCookieName,
          refreshTokenCookieName,
          cookieSecure);
    }

    @Override
    @NonNull
    public String toString() {
      return ("Security{tokenDelivery=%s, issuerServer='%s', noAuthUrls=%s, "
              + "accessTokenCookieName='%s', refreshTokenCookieName='%s', cookieSecure=%s}")
          .formatted(
              tokenDelivery,
              issuerServer,
              Arrays.toString(noAuthUrls),
              accessTokenCookieName,
              refreshTokenCookieName,
              cookieSecure);
    }
  }

  public record Bootstrap(boolean peerDatabase) {}
}
