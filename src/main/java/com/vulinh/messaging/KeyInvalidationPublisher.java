package com.vulinh.messaging;

import com.nimbusds.jose.jwk.JWKMatcher;
import com.nimbusds.jose.jwk.JWKSelector;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import com.vulinh.configuration.ApplicationProperties;
import com.vulinh.data.event.ActionUser;
import com.vulinh.data.event.EventMessageWrapper;
import com.vulinh.data.event.payload.KeyInvalidatedEvent;
import com.vulinh.data.event.payload.KeyInvalidatedEvent.Reason;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Publishes a {@code KEY_INVALIDATED} event whenever the Spring context refreshes — at JVM start
 * and on any subsequent (programmatic) {@code ConfigurableApplicationContext.refresh()}. The
 * signing key is regenerated on each refresh (see {@code SecurityConfig.jwkSource()}), so this
 * notification shortens the window where consumers reject newly-minted tokens because their JWKS
 * cache still holds the previous {@code kid}.
 *
 * <p><b>Reason discrimination.</b> The first event in any given JVM lifetime is published with
 * {@link Reason#STARTUP}; subsequent events in the same JVM (i.e., a hot context refresh) are
 * published with {@link Reason#CONTEXT_REFRESHED}. The discriminator state lives in a static
 * field so it survives the bean re-instantiation that {@code refresh()} causes — a JVM restart
 * resets it; a context refresh does not.
 *
 * <p><b>Best-effort delivery.</b> If RabbitMQ is unreachable, the failure is logged but does not
 * prevent the application from starting; consumers fall back to Spring's lazy-on-unknown-{@code
 * kid} JWKS refresh.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class KeyInvalidationPublisher {

  /**
   * JVM-wide flag distinguishing first-startup from subsequent context refreshes. Static so it
   * survives the bean re-instantiation that {@code ContextRefreshedEvent} entails — a JVM restart
   * recreates the JVM and resets it to {@code true}; a programmatic context refresh creates a new
   * publisher instance but the static field remains {@code false}.
   */
  private static final AtomicBoolean firstFireInJvm = new AtomicBoolean(true);

  private final ApplicationProperties applicationProperties;
  private final StreamBridge streamBridge;
  private final JWKSource<SecurityContext> jwkSource;

  @EventListener
  public void onContextRefreshed(ContextRefreshedEvent event) {
    // Only respond to the root context — child contexts (rare) shouldn't trigger.
    if (event.getApplicationContext().getParent() != null) {
      return;
    }

    var reason =
        firstFireInJvm.compareAndSet(true, false) ? Reason.STARTUP : Reason.CONTEXT_REFRESHED;

    publish(reason);
  }

  private void publish(Reason reason) {
    var topic = applicationProperties.messageTopic().keyInvalidated();
    var issuer = applicationProperties.security().issuerServer();
    var kid = activeKid();

    if (kid == null) {
      log.warn("No active JWK kid found; KEY_INVALIDATED event not published");
      return;
    }

    var payload = new KeyInvalidatedEvent(kid, issuer, reason);
    var wrapper =
        EventMessageWrapper.<KeyInvalidatedEvent>builder()
            .eventType(topic.type())
            .actionUser(ActionUser.SYSTEM)
            .data(payload)
            .build();

    try {
      streamBridge.send(topic.topicName(), wrapper);

      log.info(
          "Published KEY_INVALIDATED (kid={}, reason={}, topic={}, eventId={})",
          kid,
          reason,
          topic.topicName(),
          wrapper.eventId());
    } catch (Exception ex) {
      log.warn(
          "Failed to publish KEY_INVALIDATED event; consumers will fall back to lazy JWKS refresh",
          ex);
    }
  }

  private String activeKid() {
    try {
      var keys = jwkSource.get(new JWKSelector(new JWKMatcher.Builder().build()), null);
      return keys.isEmpty() ? null : keys.getFirst().getKeyID();
    } catch (Exception ex) {
      log.warn("Could not read active JWK kid", ex);
      return null;
    }
  }
}
