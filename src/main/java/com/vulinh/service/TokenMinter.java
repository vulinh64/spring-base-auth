package com.vulinh.service;

import com.vulinh.configuration.ApplicationProperties;
import com.vulinh.data.dto.TokenResult;
import com.vulinh.data.dto.TokenType;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TokenMinter {

  private final JwtEncoder jwtEncoder;
  private final ApplicationProperties applicationProperties;

  public TokenResult mint(
      String accountId,
      String clientId,
      List<String> roles,
      Duration accessTokenTtl,
      Duration refreshTokenTtl) {
    var now = Instant.now();
    var issuer = applicationProperties.security().issuerServer();

    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      var accessFuture =
          executor.submit(
              () -> encode(accessClaims(accountId, clientId, roles, now, accessTokenTtl, issuer)));

      var refreshFuture =
          executor.submit(
              () -> encode(refreshClaims(accountId, clientId, now, refreshTokenTtl, issuer)));

      return new TokenResult(
          accessFuture.get(), accessTokenTtl.getSeconds(),
          refreshFuture.get(), refreshTokenTtl.getSeconds());
    } catch (Exception e) {
      throw new IllegalStateException("Failed to generate tokens", e);
    }
  }

  private String encode(JwtClaimsSet claims) {
    return jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader(), claims)).getTokenValue();
  }

  private JwsHeader jwsHeader() {
    return JwsHeader.with(() -> applicationProperties.security().signingKey()).build();
  }

  private static JwtClaimsSet accessClaims(
      String accountId,
      String clientId,
      List<String> roles,
      Instant now,
      Duration ttl,
      String issuer) {
    return JwtClaimsSet.builder()
        .id(UUID.randomUUID().toString())
        .issuer(issuer)
        .subject(accountId)
        .audience(List.of(issuer))
        .issuedAt(now)
        .expiresAt(now.plus(ttl))
        .claim("typ", TokenType.BEARER.getTypeName())
        .claim("azp", clientId)
        .claim("resource_access", Map.of(clientId, Map.of("roles", roles)))
        .build();
  }

  private static JwtClaimsSet refreshClaims(
      String accountId, String clientId, Instant now, Duration ttl, String issuer) {
    return JwtClaimsSet.builder()
        .id(UUID.randomUUID().toString())
        .issuer(issuer)
        .subject(accountId)
        .audience(List.of(issuer))
        .issuedAt(now)
        .expiresAt(now.plus(ttl))
        .claim("typ", TokenType.REFRESH.getTypeName())
        .claim("azp", clientId)
        .build();
  }
}
