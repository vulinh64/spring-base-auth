package com.vulinh.configuration;

import com.vulinh.annotation.ExecutionTime;
import com.vulinh.data.entity.Client;
import com.vulinh.data.entity.QClient;
import com.vulinh.data.repository.ClientRepository;
import java.time.Duration;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.stereotype.Component;

// TODO: Add @CacheEvict(value = "clients", allEntries = true) when client edit endpoint is
// implemented
@Component
@RequiredArgsConstructor
public class JpaRegisteredClientRepository implements RegisteredClientRepository {

  private static final QClient Q_CLIENT = QClient.client;

  private final ClientRepository clientRepository;

  @Override
  public void save(RegisteredClient registeredClient) {
    throw new UnsupportedOperationException("Client registration is not supported");
  }

  @Override
  @Cacheable(value = "clients-by-id", key = "#id")
  public RegisteredClient findById(String id) {
    return clientRepository
        .findOne(Q_CLIENT.id.eq(UUID.fromString(id)).and(Q_CLIENT.enabled.isTrue()))
        .map(JpaRegisteredClientRepository::toRegisteredClient)
        .orElse(null);
  }

  @Override
  @Cacheable(value = "clients-by-client-id", key = "#clientId")
  @ExecutionTime
  public RegisteredClient findByClientId(String clientId) {
    return clientRepository
        .findOne(Q_CLIENT.clientId.eq(clientId).and(Q_CLIENT.enabled.isTrue()))
        .map(JpaRegisteredClientRepository::toRegisteredClient)
        .orElse(null);
  }

  private static RegisteredClient toRegisteredClient(Client client) {
    return RegisteredClient.withId(client.getId().toString())
        .clientId(client.getClientId())
        .clientName(client.getClientName())
        .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
        .authorizationGrantType(AuthorizationGrantType.JWT_BEARER)
        .tokenSettings(
            TokenSettings.builder()
                .accessTokenTimeToLive(Duration.ofSeconds(client.getAccessTokenValiditySeconds()))
                .refreshTokenTimeToLive(Duration.ofSeconds(client.getRefreshTokenValiditySeconds()))
                .build())
        .build();
  }
}
