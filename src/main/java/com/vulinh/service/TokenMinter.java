package com.vulinh.service;

import com.vulinh.configuration.ApplicationProperties;
import com.vulinh.data.dto.RoleResponse;
import com.vulinh.data.dto.TokenResult;
import com.vulinh.data.dto.TokenType;
import com.vulinh.data.entity.Account;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JoseHeaderNames;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtClaimsSet.Builder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TokenMinter {

  private final JwtEncoder jwtEncoder;
  private final ApplicationProperties applicationProperties;

  public TokenResult mint(Account account, List<String> roles, RegisteredClient client) {
    var issuer = applicationProperties.security().issuerServer();

    var tokenSettings = client.getTokenSettings();

    var accessTokenTtl = tokenSettings.getAccessTokenTimeToLive();
    var refreshTokenTtl = tokenSettings.getRefreshTokenTimeToLive();

    var clientId = client.getClientId();

    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      var accessFuture =
          executor.submit(
              () -> encode(accessClaims(account, clientId, roles, accessTokenTtl, issuer)));

      var refreshFuture =
          executor.submit(() -> encode(refreshClaims(account, clientId, refreshTokenTtl, issuer)));

      return new TokenResult(
          accessFuture.get(), accessTokenTtl.getSeconds(),
          refreshFuture.get(), refreshTokenTtl.getSeconds());
    } catch (Exception e) {
      // Something is very wrong here
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Failed to generate tokens", e);
    }
  }

  private String encode(JwtClaimsSet claims) {
    return jwtEncoder
        .encode(JwtEncoderParameters.from(JwsHeader.with(SignatureAlgorithm.RS256).build(), claims))
        .getTokenValue();
  }

  private static JwtClaimsSet accessClaims(
      Account account, String clientId, List<String> roles, Duration ttl, String issuer) {
    return commonClaimsBuilder(account, clientId, ttl, issuer)
        .claim(JoseHeaderNames.TYP, TokenType.BEARER.getTypeName())
        .claim("resource_access", Map.ofEntries(Map.entry(clientId, new RoleResponse(roles))))
        .build();
  }

  private static JwtClaimsSet refreshClaims(
      Account accountId, String clientId, Duration ttl, String issuer) {
    return commonClaimsBuilder(accountId, clientId, ttl, issuer)
        .claim(JoseHeaderNames.TYP, TokenType.REFRESH.getTypeName())
        .build();
  }

  private static Builder commonClaimsBuilder(
      Account account, String clientId, Duration ttl, String issuer) {
    var now = Instant.now();

    return JwtClaimsSet.builder()
        .id(UUID.randomUUID().toString())
        .issuer(issuer)
        .subject(account.getId().toString())
        .audience(List.of(issuer))
        .issuedAt(now)
        .expiresAt(now.plus(ttl))
        .claim("azp", clientId);
  }
}
