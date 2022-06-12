package com.e2e.api;

import java.util.function.Consumer;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.boot.test.mock.mockito.MockBean;

@TestComponent
class TestRabbitListener {

  @MockBean
  private Consumer<String> messageConsumer;

  @RabbitListener(queues = "${queue.name}")
  public void listen(String message) {
    messageConsumer.accept(message);
  }
}
