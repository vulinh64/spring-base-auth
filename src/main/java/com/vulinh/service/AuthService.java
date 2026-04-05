package com.vulinh.service;

import com.vulinh.annotation.ExecutionTime;
import com.vulinh.configuration.ApplicationProperties;
import com.vulinh.data.dto.LoginRequest;
import com.vulinh.data.dto.RefreshRequest;
import com.vulinh.data.dto.TokenResult;
import com.vulinh.data.dto.TokenType;
import com.vulinh.data.entity.AccountCredential.CredentialType;
import com.vulinh.data.entity.ClientRole;
import com.vulinh.data.repository.AccountCredentialRepository;
import com.vulinh.data.repository.AccountRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

  private final AccountRepository accountRepository;
  private final AccountCredentialRepository credentialRepository;
  private final RegisteredClientRepository registeredClientRepository;
  private final ApplicationProperties applicationProperties;

  private final JwtEncoder jwtEncoder;
  private final JwtDecoder jwtDecoder;

  private final PasswordVerifier passwordVerifier;
  private final BruteForceProtection bruteForceProtection;

  @ExecutionTime
  public TokenResult login(LoginRequest request) {
    bruteForceProtection.checkLocked(request.username());

    var client =
        Optional.ofNullable(registeredClientRepository.findByClientId(request.clientId()))
            .orElseThrow(() -> new IllegalArgumentException("Invalid client"));

    var credentialType =
        switch (request.grantType()) {
          case "password" -> CredentialType.PASSWORD;
          case "otp" -> CredentialType.OTP;
          default -> throw new IllegalArgumentException("Unsupported grant type");
        };

    var account =
        accountRepository
            .fetchForLogin(request.username(), credentialType)
            .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

    var credential =
        account.getCredentials().stream()
            .filter(c -> c.getCredentialType() == credentialType && c.isEnabled())
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

    switch (credentialType) {
      case PASSWORD -> {
        if (!passwordVerifier.matches(request.password(), credential.getMetadata())) {
          bruteForceProtection.recordFailure(request.username());
          throw new IllegalArgumentException("Invalid credentials");
        }
      }
      case OTP -> {
        if (credential.getExpiresAt() != null && Instant.now().isAfter(credential.getExpiresAt())) {
          bruteForceProtection.recordFailure(request.username());
          throw new IllegalArgumentException("OTP expired");
        }
        if (!credential.getMetadata().equals(hashOtp(request.otp()))) {
          bruteForceProtection.recordFailure(request.username());
          throw new IllegalArgumentException("Invalid credentials");
        }
        credential.setEnabled(false);
        credentialRepository.save(credential);
      }
    }

    bruteForceProtection.recordSuccess(request.username());

    var clientUuid = UUID.fromString(client.getId());
    var tokenSettings = client.getTokenSettings();
    var accessTokenTtl = tokenSettings.getAccessTokenTimeToLive();
    var refreshTokenTtl = tokenSettings.getRefreshTokenTimeToLive();

    var roles =
        account.getClientRoles().stream()
            .filter(role -> clientUuid.equals(role.getClientId()))
            .map(ClientRole::getRoleName)
            .toList();

    var now = Instant.now();
    var issuer = applicationProperties.security().issuerServer();

    var accountId = account.getId().toString();
    var clientIdStr = request.clientId();

    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      var accessFuture =
          executor.submit(
              () ->
                  jwtEncoder
                      .encode(
                          JwtEncoderParameters.from(
                              JwsHeader.with(SignatureAlgorithm.ES256).build(),
                              JwtClaimsSet.builder()
                                  .id(UUID.randomUUID().toString())
                                  .issuer(issuer)
                                  .subject(accountId)
                                  .audience(List.of(issuer))
                                  .issuedAt(now)
                                  .expiresAt(now.plus(accessTokenTtl))
                                  .claim("typ", TokenType.BEARER.getTypeName())
                                  .claim("azp", clientIdStr)
                                  .claim("resource_access", Map.of(clientIdStr, Map.of("roles", roles)))
                                  .build()))
                      .getTokenValue());

      var refreshFuture =
          executor.submit(
              () ->
                  jwtEncoder
                      .encode(
                          JwtEncoderParameters.from(
                              JwsHeader.with(SignatureAlgorithm.ES256).build(),
                              JwtClaimsSet.builder()
                                  .id(UUID.randomUUID().toString())
                                  .issuer(issuer)
                                  .subject(accountId)
                                  .audience(List.of(issuer))
                                  .issuedAt(now)
                                  .expiresAt(now.plus(refreshTokenTtl))
                                  .claim("azp", clientIdStr)
                                  .claim("typ", TokenType.REFRESH.getTypeName())
                                  .build()))
                      .getTokenValue());

      return new TokenResult(
          accessFuture.get(), (int) accessTokenTtl.getSeconds(),
          refreshFuture.get(), (int) refreshTokenTtl.getSeconds());
    } catch (Exception e) {
      throw new IllegalStateException("Failed to generate tokens", e);
    }
  }

  @ExecutionTime
  public TokenResult refresh(RefreshRequest request) {
    var jwt = jwtDecoder.decode(request.refreshToken());

    if (!TokenType.REFRESH.getTypeName().equals(jwt.getClaimAsString("typ"))) {
      throw new IllegalArgumentException("Invalid token type");
    }

    var accountId = jwt.getSubject();
    var clientIdStr = jwt.getClaimAsString("azp");

    var client =
        Optional.ofNullable(registeredClientRepository.findByClientId(clientIdStr))
            .orElseThrow(() -> new IllegalArgumentException("Invalid client"));

    var clientUuid = UUID.fromString(client.getId());
    var tokenSettings = client.getTokenSettings();
    var accessTokenTtl = tokenSettings.getAccessTokenTimeToLive();
    var refreshTokenTtl = tokenSettings.getRefreshTokenTimeToLive();

    var account =
        accountRepository
            .findById(UUID.fromString(accountId))
            .orElseThrow(() -> new IllegalArgumentException("Invalid account"));

    var roles =
        account.getClientRoles().stream()
            .filter(role -> clientUuid.equals(role.getClientId()))
            .map(ClientRole::getRoleName)
            .toList();

    var now = Instant.now();
    var issuer = applicationProperties.security().issuerServer();

    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      var accessFuture =
          executor.submit(
              () ->
                  jwtEncoder
                      .encode(
                          JwtEncoderParameters.from(
                              JwsHeader.with(SignatureAlgorithm.ES256).build(),
                              JwtClaimsSet.builder()
                                  .id(UUID.randomUUID().toString())
                                  .issuer(issuer)
                                  .subject(accountId)
                                  .audience(List.of(issuer))
                                  .issuedAt(now)
                                  .expiresAt(now.plus(accessTokenTtl))
                                  .claim("typ", TokenType.BEARER.getTypeName())
                                  .claim("azp", clientIdStr)
                                  .claim("resource_access", Map.of(clientIdStr, Map.of("roles", roles)))
                                  .build()))
                      .getTokenValue());

      var refreshFuture =
          executor.submit(
              () ->
                  jwtEncoder
                      .encode(
                          JwtEncoderParameters.from(
                              JwsHeader.with(SignatureAlgorithm.ES256).build(),
                              JwtClaimsSet.builder()
                                  .id(UUID.randomUUID().toString())
                                  .issuer(issuer)
                                  .subject(accountId)
                                  .audience(List.of(issuer))
                                  .issuedAt(now)
                                  .expiresAt(now.plus(refreshTokenTtl))
                                  .claim("azp", clientIdStr)
                                  .claim("typ", TokenType.REFRESH.getTypeName())
                                  .build()))
                      .getTokenValue());

      return new TokenResult(
          accessFuture.get(), (int) accessTokenTtl.getSeconds(),
          refreshFuture.get(), (int) refreshTokenTtl.getSeconds());
    } catch (Exception e) {
      throw new IllegalStateException("Failed to generate tokens", e);
    }
  }

  private static String hashOtp(String otp) {
    try {
      var hash = MessageDigest.getInstance("SHA-256").digest(otp.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hash);
    } catch (java.security.NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 not available", e);
    }
  }
}
