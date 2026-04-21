package com.vulinh.configuration;

import java.util.Arrays;
import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.lang.NonNull;

@ConfigurationProperties(prefix = "application-properties")
public record ApplicationProperties(Security security, Bootstrap bootstrap) {

  public record Security(
      TokenDelivery tokenDelivery, String issuerServer, String signingKey, String[] noAuthUrls) {

    public enum TokenDelivery {
      COOKIE,
      HEADER,
      BODY
    }

    @Override
    public boolean equals(Object other) {
      if (other
          instanceof
          Security(TokenDelivery delivery, String server, String key, String[] authUrls)) {
        return Objects.equals(signingKey, key)
            && Objects.equals(issuerServer, server)
            && Objects.deepEquals(noAuthUrls, authUrls)
            && tokenDelivery == delivery;
      }
      return false;
    }

    @Override
    public int hashCode() {
      return Objects.hash(tokenDelivery, issuerServer, signingKey, Arrays.hashCode(noAuthUrls));
    }

    @Override
    @NonNull
    public String toString() {
      return "Security{tokenDelivery=%s, issuerServer='%s', signingKey='%s', noAuthUrls=%s}"
          .formatted(tokenDelivery, issuerServer, signingKey, Arrays.toString(noAuthUrls));
    }
  }

  public record Bootstrap(boolean peerDatabase) {}
}
