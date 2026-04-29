package com.vulinh.service;

import com.vulinh.configuration.ApplicationProperties;
import com.vulinh.data.dto.AccessTokenResult;
import com.vulinh.data.dto.TokenResult;
import com.vulinh.data.dto.TokenType;
import com.vulinh.data.entity.Account;
import com.vulinh.data.entity.Client;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtClaimsSet.Builder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TokenMinter {

  public static final String SESSION_AUDIENCE = "session";

  private final JwtEncoder jwtEncoder;
  private final ApplicationProperties applicationProperties;

  /** Used by /login and /refresh: mints session_token + refresh_token in parallel. */
  public TokenResult mintSessionPair(Account account, Client originatingClient) {
    var sessionTtl = Duration.ofSeconds(originatingClient.getAccessTokenValiditySeconds());
    var refreshTtl = Duration.ofSeconds(originatingClient.getRefreshTokenValiditySeconds());
    var clientId = originatingClient.getClientId();

    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      var sessionFuture =
          executor.submit(() -> encode(sessionClaims(account, clientId, sessionTtl)));
      var refreshFuture =
          executor.submit(() -> encode(refreshClaims(account, clientId, refreshTtl)));

      return new TokenResult(
          sessionFuture.get(), sessionTtl.getSeconds(),
          refreshFuture.get(), refreshTtl.getSeconds());
    } catch (Exception e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Failed to mint session token pair", e);
    }
  }

  /** Used by /exchange: mints an audience-scoped access_token for one target client. */
  public AccessTokenResult mintAccess(
      Account account, Client originatingClient, Client targetClient, List<String> roles) {
    var ttl = Duration.ofSeconds(targetClient.getAccessTokenValiditySeconds());
    var token = encode(accessClaims(account, originatingClient, targetClient, roles, ttl));
    return new AccessTokenResult(token, ttl.getSeconds(), targetClient.getClientId());
  }

  private String encode(JwtClaimsSet claims) {
    return jwtEncoder
        .encode(JwtEncoderParameters.from(JwsHeader.with(SignatureAlgorithm.RS256).build(), claims))
        .getTokenValue();
  }

  private JwtClaimsSet sessionClaims(Account account, String clientId, Duration ttl) {
    return commonBuilder(account, clientId, ttl, SESSION_AUDIENCE)
        .claim("typ", TokenType.SESSION.getTypeName())
        .build();
  }

  private JwtClaimsSet refreshClaims(Account account, String clientId, Duration ttl) {
    return commonBuilder(account, clientId, ttl, SESSION_AUDIENCE)
        .claim("typ", TokenType.REFRESH.getTypeName())
        .build();
  }

  private JwtClaimsSet accessClaims(
      Account account,
      Client originatingClient,
      Client targetClient,
      List<String> roles,
      Duration ttl) {
    // Username is included only on access tokens, for log-line / audit readability.
    // Session and refresh tokens stay minimal.
    return commonBuilder(account, originatingClient.getClientId(), ttl, targetClient.getClientId())
        .claim("typ", TokenType.ACCESS.getTypeName())
        .claim("username", account.getUsername())
        .claim("roles", roles)
        .build();
  }

  private Builder commonBuilder(Account account, String azp, Duration ttl, String audience) {
    var now = Instant.now();
    return JwtClaimsSet.builder()
        .id(UUID.randomUUID().toString())
        .issuer(applicationProperties.security().issuerServer())
        .subject(account.getId().toString())
        .audience(List.of(audience))
        .issuedAt(now)
        .expiresAt(now.plus(ttl))
        .claim(IdTokenClaimNames.AZP, azp);
  }
}
