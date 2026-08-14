package com.vulinh;

import com.github.dockerjava.api.command.InspectContainerResponse;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.ConnectionFactory;
import com.vulinh.test.BaseContainersConfiguration;
import com.vulinh.test.BaseDockerImage;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.rabbitmq.RabbitMQContainer;

@TestConfiguration(proxyBeanMethods = false)
public class ContainersConfiguration extends BaseContainersConfiguration {

  public static final String KEY_INVALIDATION_QUEUE_NAME = "key-invalidation-startup-it";
  private static final String KEY_INVALIDATION_TOPIC_NAME = "key-invalidated";

  @Override
  @Bean
  @ServiceConnection
  protected RabbitMQContainer rabbitmqContainer() {
    return new RabbitMQContainer(BaseDockerImage.RABBITMQ_IMAGE) {

      @Override
      protected void containerIsStarted(InspectContainerResponse containerInfo, boolean reused) {
        super.containerIsStarted(containerInfo, reused);
        declareKeyInvalidationQueue(this);
      }
    };
  }

  private static void declareKeyInvalidationQueue(RabbitMQContainer container) {
    var factory = new ConnectionFactory();
    factory.setHost(container.getHost());
    factory.setPort(container.getAmqpPort());
    factory.setUsername(container.getAdminUsername());
    factory.setPassword(container.getAdminPassword());

    try (var connection = factory.newConnection();
        var channel = connection.createChannel()) {
      channel.exchangeDeclare(KEY_INVALIDATION_TOPIC_NAME, BuiltinExchangeType.TOPIC, true);
      channel.queueDeclare(KEY_INVALIDATION_QUEUE_NAME, false, false, false, null);
      channel.queueBind(KEY_INVALIDATION_QUEUE_NAME, KEY_INVALIDATION_TOPIC_NAME, "#");
    } catch (Exception ex) {
      throw new IllegalStateException("Failed to declare startup invalidation queue", ex);
    }
  }
}
