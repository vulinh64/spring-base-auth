package com.vulinh.service;

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

  private final TokenMinter tokenMinter;
  private final CredentialStrategies credentialStrategies;

  private final AccountRepository accountRepository;
  private final RegisteredClientRepository registeredClientRepository;
  private final JwtDecoder jwtDecoder;

  public TokenResult login(LoginRequest request) {
    var client =
        Optional.ofNullable(
                registeredClientRepository.findByClientId(String.valueOf(request.clientId())))
            .orElseThrow(() -> new IllegalArgumentException("Invalid client"));

    var credentialType = CredentialType.fromGrantType(request.grantType());

    var account =
        accountRepository
            .fetchForLogin(request.username(), credentialType)
            .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

    var credential =
        account.getCredentials().stream()
            .filter(
                credentials ->
                    credentials.getCredentialType() == credentialType && credentials.isEnabled())
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

    credentialStrategies.forType(credentialType).verify(request, credential);

    var roles = accountRepository.findRoleNames(account.getId(), UUID.fromString(client.getId()));

    return tokenMinter.mint(account, roles, client);
  }

  public TokenResult refresh(RefreshRequest request) {
    var jwt = jwtDecoder.decode(request.refreshToken());

    if (!TokenType.REFRESH.getTypeName().equals(jwt.getClaimAsString("typ"))) {
      throw new IllegalArgumentException("Invalid token type");
    }

    var accountId = jwt.getSubject();
    var clientId = jwt.getClaimAsString("azp");

    var client =
        Optional.ofNullable(registeredClientRepository.findByClientId(clientId))
            .orElseThrow(() -> new IllegalArgumentException("Invalid client"));

    var accountUuid = UUID.fromString(accountId);

    var account =
        accountRepository
            .findById(accountUuid)
            .orElseThrow(() -> new IllegalArgumentException("Invalid account"));

    var clientUuid = UUID.fromString(clientId);

    var roles = accountRepository.findRoleNames(accountUuid, clientUuid);

    return tokenMinter.mint(account, roles, client);
  }
}
