package com.vulinh.service;

import com.vulinh.annotation.ExecutionTime;
import com.vulinh.data.dto.LoginRequest;
import com.vulinh.data.dto.RefreshRequest;
import com.vulinh.data.dto.TokenResult;
import com.vulinh.data.dto.TokenType;
import com.vulinh.data.entity.AccountCredential.CredentialType;
import com.vulinh.data.repository.AccountRepository;
import com.vulinh.service.credential.CredentialStrategies;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

  private final AccountRepository accountRepository;
  private final RegisteredClientRepository registeredClientRepository;
  private final JwtDecoder jwtDecoder;
  private final TokenMinter tokenMinter;
  private final CredentialStrategies credentialStrategies;
  private final BruteForceProtection bruteForceProtection;

  @ExecutionTime
  public TokenResult login(LoginRequest request) {
    bruteForceProtection.checkLocked(request.username());

    var client =
        Optional.ofNullable(registeredClientRepository.findByClientId(request.clientId()))
            .orElseThrow(() -> new IllegalArgumentException("Invalid client"));

    var credentialType = CredentialType.fromGrantType(request.grantType());

    var account =
        accountRepository
            .fetchForLogin(request.username(), credentialType)
            .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

    var credential =
        account.getCredentials().stream()
            .filter(c -> c.getCredentialType() == credentialType && c.isEnabled())
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

    try {
      credentialStrategies.forType(credentialType).verify(request, credential);
    } catch (RuntimeException e) {
      bruteForceProtection.recordFailure(request.username());
      throw e;
    }

    bruteForceProtection.recordSuccess(request.username());

    var clientUuid = UUID.fromString(client.getId());
    var roles = accountRepository.findRoleNames(account.getId(), clientUuid);
    var tokenSettings = client.getTokenSettings();

    return tokenMinter.mint(
        account.getId().toString(),
        request.clientId(),
        roles,
        tokenSettings.getAccessTokenTimeToLive(),
        tokenSettings.getRefreshTokenTimeToLive());
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

    var accountUuid = UUID.fromString(accountId);
    var clientUuid = UUID.fromString(client.getId());

    accountRepository
        .findById(accountUuid)
        .orElseThrow(() -> new IllegalArgumentException("Invalid account"));

    var roles = accountRepository.findRoleNames(accountUuid, clientUuid);
    var tokenSettings = client.getTokenSettings();

    return tokenMinter.mint(
        accountId,
        clientIdStr,
        roles,
        tokenSettings.getAccessTokenTimeToLive(),
        tokenSettings.getRefreshTokenTimeToLive());
  }
}
