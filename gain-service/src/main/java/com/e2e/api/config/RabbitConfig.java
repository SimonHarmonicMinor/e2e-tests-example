package com.e2e.api.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@EnableRabbit
@Configuration
public class RabbitConfig {

  @Value("${queue.input.name}")
  private String inputQueue;
  @Value("${queue.output.name}")
  private String outputQueue;

  @Bean
  public Queue inputQueue() {
    return new Queue(inputQueue, false);
  }

  @Bean
  public Queue outputQueue() {
    return new Queue(outputQueue, false);
  }
}
