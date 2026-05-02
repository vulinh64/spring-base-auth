package com.vulinh.service;

import com.vulinh.data.ServiceCodeError;
import com.vulinh.data.dto.AccessTokenResult;
import com.vulinh.data.dto.LoginRequest;
import com.vulinh.data.dto.RefreshRequest;
import com.vulinh.data.dto.TokenResult;
import com.vulinh.data.dto.TokenType;
import com.vulinh.data.entity.AccountCredential.CredentialType;
import com.vulinh.data.repository.AccountRepository;
import com.vulinh.data.repository.ClientRepository;
import com.vulinh.exception.ApplicationValidationException;
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
                    new ApplicationValidationException(
                        "Invalid client_id: %s".formatted(request.clientId()),
                        ServiceCodeError.INVALID_CLIENT));

    var credentialType = CredentialType.fromGrantType(request.grantType());
    var account = credentialStrategies.forType(credentialType).verify(request).getAccount();

    return tokenMinter.mintSessionPair(account, client);
  }

  public TokenResult refresh(RefreshRequest request) {
    var jwt = jwtDecoder.decode(request.refreshToken());

    if (!TokenType.REFRESH.getTypeName().equals(jwt.getClaimAsString("typ"))) {
      throw new ApplicationValidationException(
          "Refresh endpoint expected typ=refresh", ServiceCodeError.INVALID_TOKEN_TYPE);
    }
    if (!jwt.getAudience().contains(TokenMinter.SESSION_AUDIENCE)) {
      throw new ApplicationValidationException(
          "Refresh endpoint expected aud=session", ServiceCodeError.INVALID_AUDIENCE);
    }

    var clientId = jwt.getClaimAsString(IdTokenClaimNames.AZP);
    var client =
        clientRepository
            .findByClientIdAndEnabledIsTrue(clientId)
            .orElseThrow(
                () ->
                    new ApplicationValidationException(
                        "Invalid client_id: %s".formatted(clientId),
                        ServiceCodeError.INVALID_CLIENT));

    var account =
        accountRepository
            .findById(UUID.fromString(jwt.getSubject()))
            .orElseThrow(
                () ->
                    new ApplicationValidationException(
                        "Account not found for sub: %s".formatted(jwt.getSubject()),
                        ServiceCodeError.INVALID_ACCOUNT));

    return tokenMinter.mintSessionPair(account, client);
  }

  public AccessTokenResult exchange(String sessionToken, String audience) {
    var jwt = jwtDecoder.decode(sessionToken);

    if (!TokenType.SESSION.getTypeName().equals(jwt.getClaimAsString("typ"))) {
      throw new ApplicationValidationException(
          "Exchange endpoint expected typ=session", ServiceCodeError.INVALID_TOKEN_TYPE);
    }
    if (!jwt.getAudience().contains(TokenMinter.SESSION_AUDIENCE)) {
      throw new ApplicationValidationException(
          "Exchange endpoint expected aud=session", ServiceCodeError.INVALID_AUDIENCE);
    }

    var originatingClientId = jwt.getClaimAsString(IdTokenClaimNames.AZP);

    var originatingClient =
        clientRepository
            .findByClientIdAndEnabledIsTrue(originatingClientId)
            .orElseThrow(
                () ->
                    new ApplicationValidationException(
                        "Originating client not found: %s".formatted(originatingClientId),
                        ServiceCodeError.INVALID_CLIENT));

    var targetClient =
        clientRepository
            .findByClientIdAndEnabledIsTrue(audience)
            .orElseThrow(
                () ->
                    new ApplicationValidationException(
                        "Target audience not found: %s".formatted(audience),
                        ServiceCodeError.INVALID_TARGET_AUDIENCE));

    var accountId = UUID.fromString(jwt.getSubject());
    var account =
        accountRepository
            .findById(accountId)
            .orElseThrow(
                () ->
                    new ApplicationValidationException(
                        "Account not found for sub: %s".formatted(accountId),
                        ServiceCodeError.INVALID_ACCOUNT));

    var roles = accountRepository.findRoleNames(accountId, audience);

    return tokenMinter.mintAccess(account, originatingClient, targetClient, roles);
  }
}
