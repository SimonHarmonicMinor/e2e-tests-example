package com.e2e.tests.util;

import static org.testcontainers.shaded.com.google.common.base.Charsets.UTF_8;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import lombok.Getter;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestComponent;
import org.testcontainers.shaded.com.google.common.base.Objects;

@TestComponent
@Getter
public class TestRabbitListener {

  @Value("${queue.api}")
  private String apiQueue;
  @Value("${queue.gain}")
  private String gainQueue;

  private final Map<QueueType, List<String>> messages = new ConcurrentHashMap<>();

  @RabbitListener(queues = {"${queue.api}", "${queue.gain}"})
  public void listenTopics(Message message) {
    final var queue = message.getMessageProperties().getConsumerQueue();
    final var body = new String(message.getBody(), UTF_8);
    final var queueType = getQueueType(queue);
    messages.computeIfAbsent(
            queueType,
            k -> new CopyOnWriteArrayList<>()
        )
        .add(body);
  }

  private QueueType getQueueType(String queue) {
    if (Objects.equal(apiQueue, queue)) {
      return QueueType.API;
    } else if (Objects.equal(gainQueue, queue)) {
      return QueueType.GAIN;
    }
    throw new IllegalArgumentException("Unknown queue: " + queue);
  }

  public enum QueueType {
    API, GAIN
  }
}
