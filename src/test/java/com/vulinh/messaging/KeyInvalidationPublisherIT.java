package com.vulinh.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import module java.base;

import com.nimbusds.jose.jwk.JWKMatcher;
import com.nimbusds.jose.jwk.JWKSelector;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import com.vulinh.AuthIntegrationTestBase;
import com.vulinh.ContainersConfiguration;
import com.vulinh.configuration.ApplicationProperties;
import com.vulinh.data.event.EventMessageWrapper;
import com.vulinh.data.event.EventType;
import com.vulinh.data.event.payload.KeyInvalidatedEvent;
import com.vulinh.utils.JsonUtils;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.core.type.TypeReference;

class KeyInvalidationPublisherIT extends AuthIntegrationTestBase {

  private static final long RECEIVE_TIMEOUT_MILLIS = 10_000;

  @Autowired private RabbitTemplate rabbitTemplate;

  @Autowired private ApplicationProperties applicationProperties;

  @Autowired private JWKSource<SecurityContext> jwkSource;

  @Test
  void appStartupPublishesActiveSigningKidInvalidation() throws Exception {
    var message =
        rabbitTemplate.receive(
            ContainersConfiguration.KEY_INVALIDATION_QUEUE_NAME, RECEIVE_TIMEOUT_MILLIS);

    assertNotNull(message, "Expected a KEY_INVALIDATED event during application startup");

    var event =
        JsonUtils.toObject(
            new String(message.getBody(), StandardCharsets.UTF_8),
            new TypeReference<EventMessageWrapper<KeyInvalidatedEvent>>() {});

    assertEquals(EventType.KEY_INVALIDATED, event.eventType());
    assertEquals(activeKid(), event.data().kid());
    assertEquals(applicationProperties.security().issuerServer(), event.data().issuer());
  }

  private String activeKid() throws Exception {
    return jwkSource
        .get(new JWKSelector(new JWKMatcher.Builder().build()), null)
        .getFirst()
        .getKeyID();
  }
}
