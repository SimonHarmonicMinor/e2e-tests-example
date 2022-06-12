package com.e2e.api.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@EnableRabbit
@Configuration
public class RabbitConfig {
  @Value("${queue.name}")
  private String queueName;

  @Bean
  public Queue queue() {
    return new Queue(queueName, false);
  }
}
