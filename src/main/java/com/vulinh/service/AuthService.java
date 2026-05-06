package com.vulinh.service;

import com.vulinh.data.dto.LoginRequest;
import com.vulinh.data.dto.RefreshRequest;
import com.vulinh.data.dto.TokenResult;
import com.vulinh.data.dto.TokenType;
import com.vulinh.data.entity.Account;
import com.vulinh.data.entity.AccountCredential.CredentialType;
import com.vulinh.data.repository.AccountRepository;
import com.vulinh.data.repository.ClientRepository;
import com.vulinh.exception.AccountDisabledException;
import com.vulinh.exception.ClientNotFoundException;
import com.vulinh.exception.InvalidTokenException;
import com.vulinh.service.credential.CredentialStrategies;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

  private final TokenMinter tokenMinter;
  private final CredentialStrategies credentialStrategies;
  private final AccountRepository accountRepository;
  private final ClientRepository clientRepository;
  private final JwtDecoder jwtDecoder;

  @Transactional
  public TokenResult login(LoginRequest request) {
    var client =
        clientRepository
            .findByClientIdAndEnabledIsTrue(request.clientId())
            .orElseThrow(
                () ->
                    new ClientNotFoundException(
                        "Invalid client_id: %s".formatted(request.clientId())));

    var credentialType = CredentialType.fromGrantType(request.grantType());

    var account = credentialStrategies.forType(credentialType).verify(request).getAccount();

    var roles = accountRepository.findRoleNames(account.getId(), client.getClientId());

    return tokenMinter.mintTokenPair(account, client, roles);
  }

  public TokenResult refresh(RefreshRequest request) {
    var jwt = jwtDecoder.decode(request.refreshToken());

    if (!TokenType.REFRESH.getTypeName().equals(jwt.getClaimAsString("typ"))) {
      throw new InvalidTokenException("Refresh endpoint expected typ=refresh");
    }

    var clientId = jwt.getClaimAsString(IdTokenClaimNames.AZP);
    var client =
        clientRepository
            .findByClientIdAndEnabledIsTrue(clientId)
            .orElseThrow(
                () -> new ClientNotFoundException("Invalid client_id: %s".formatted(clientId)));

    var accountId = UUID.fromString(jwt.getSubject());

    var account =
        accountRepository
            .findById(accountId)
            .filter(Account::isAccountEnabled)
            .orElseThrow(
                () ->
                    new AccountDisabledException(
                        "Account [%s] not active".formatted(accountId)));

    var roles = accountRepository.findRoleNames(account.getId(), clientId);

    return tokenMinter.mintTokenPair(account, client, roles);
  }
}
