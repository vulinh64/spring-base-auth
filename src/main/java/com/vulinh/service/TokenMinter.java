package com.vulinh.service;

import com.vulinh.configuration.ApplicationProperties;
import com.vulinh.data.dto.TokenResult;
import com.vulinh.data.dto.TokenType;
import com.vulinh.data.dto.AccountInfo;
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

  private final JwtEncoder jwtEncoder;
  private final ApplicationProperties applicationProperties;

  /** Used by /login and /refresh: mints access_token + refresh_token in parallel. */
  public TokenResult mintTokenPair(Account account, Client client, List<String> roles) {
    var accessTtl = Duration.ofSeconds(client.getAccessTokenValiditySeconds());
    var refreshTtl = Duration.ofSeconds(client.getRefreshTokenValiditySeconds());
    var clientId = client.getClientId();

    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      var accessFuture =
          executor.submit(() -> encode(accessClaims(account, clientId, roles, accessTtl)));
      var refreshFuture =
          executor.submit(() -> encode(refreshClaims(account, clientId, refreshTtl)));

      return TokenResult.full(
          AccountInfo.from(account, roles),
          accessFuture.get(), accessTtl.getSeconds(),
          refreshFuture.get(), refreshTtl.getSeconds());
    } catch (Exception e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Failed to mint token pair", e);
    }
  }

  private String encode(JwtClaimsSet claims) {
    return jwtEncoder
        .encode(JwtEncoderParameters.from(JwsHeader.with(SignatureAlgorithm.RS256).build(), claims))
        .getTokenValue();
  }

  private JwtClaimsSet accessClaims(
      Account account, String clientId, List<String> roles, Duration ttl) {
    return commonBuilder(account, clientId, ttl)
        .claim("typ", TokenType.ACCESS.getTypeName())
        .claim("username", account.getUsername())
        .claim("roles", roles)
        .build();
  }

  private JwtClaimsSet refreshClaims(Account account, String clientId, Duration ttl) {
    return commonBuilder(account, clientId, ttl)
        .claim("typ", TokenType.REFRESH.getTypeName())
        .build();
  }

  // PoC: aud == azp == clientId. Revisit with a "sensible" aud strategy later.
  private Builder commonBuilder(Account account, String clientId, Duration ttl) {
    var now = Instant.now();
    return JwtClaimsSet.builder()
        .id(UUID.randomUUID().toString())
        .issuer(applicationProperties.security().issuerServer())
        .subject(account.getId().toString())
        .audience(List.of(clientId))
        .issuedAt(now)
        .expiresAt(now.plus(ttl))
        .claim(IdTokenClaimNames.AZP, clientId);
  }
}
