package com.e2e.api;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class Listener {

  private final RabbitTemplate rabbitTemplate;
  private final RedisTemplate<String, String> redisTemplate;
  private final ObjectMapper objectMapper;
  private final String outputQueue;

  public Listener(
      RabbitTemplate rabbitTemplate,
      RedisTemplate<String, String> redisTemplate,
      ObjectMapper objectMapper,
      @Value("${queue.output.name}")
          String outputQueue
  ) {
    this.rabbitTemplate = rabbitTemplate;
    this.redisTemplate = redisTemplate;
    this.objectMapper = objectMapper;
    this.outputQueue = outputQueue;
  }

  @RabbitListener(queues = "${queue.input.name}")
  @SneakyThrows
  @SuppressWarnings("unchecked")
  public void listenMessage(String message) {
    log.info("Message accepted message='{}'", message);
    Map<String, Object> messageMap = new HashMap<String, Object>(
        objectMapper.readValue(message, Map.class)
    );
    Object cookieObj = messageMap.get("cookie");
    Object msisdnObj = messageMap.get("msisdn");

    if (cookieObj instanceof String cookie
        && msisdnObj instanceof String msisdn) {
      redisTemplate.opsForHash().put("cookie-to-msisdn", cookie, msisdn);
    } else if (cookieObj instanceof String cookie) {
      String msisdn = (String) redisTemplate.opsForHash().get("cookie-to-msisdn", cookie);
      messageMap.put("msisdn", msisdn);
    }
    rabbitTemplate.send(outputQueue, new Message(
        objectMapper.writeValueAsString(messageMap).getBytes(UTF_8)
    ));
    log.info("Message sent to RabbitMQ {topic='{}', message='{}'}", outputQueue, messageMap);
  }
}
