package com.e2e.api;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageSender {
  @Value("${queue.name}")
  private String queueName;
  private final RabbitTemplate rabbitTemplate;
  private final ObjectMapper objectMapper;

  @SneakyThrows
  public void sendMessage(Map<String, Object> message) {
    rabbitTemplate.send(
        queueName,
        new Message(
            objectMapper.writeValueAsString(message)
                .getBytes(UTF_8)
        )
    );
    log.info("Message sent to RabbitMQ {topic='{}', message='{}'}", queueName, message);
  }
}
