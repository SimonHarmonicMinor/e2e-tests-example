package com.e2e.api;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import com.e2e.api.testutils.FunctionalTestSuite;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.function.Consumer;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;

@Import(TestRabbitListener.class)
class ListenerFunctionalTest extends FunctionalTestSuite {

  @Autowired
  private RabbitTemplate rabbitTemplate;
  @Autowired
  private RedisTemplate<String, String> redisTemplate;
  @Value("${queue.input.name}")
  private String inputQueue;
  @Autowired
  private ObjectMapper objectMapper;
  @Autowired
  private Consumer<String> messageConsumer;

  @BeforeEach
  void beforeEach() {
    redisTemplate.delete("cookie-to-msisdn");
  }

  @Test
  void shouldForwardMessageAsIs() {
    rabbitTemplate.send(inputQueue, newMessage(
        Map.of("key", "value")
    ));

    verify(messageConsumer, timeout(5000).times(1)).accept(
        argThat(received -> {
          Map<String, Object> map = jsonToMap(received);
          assertEquals(
              Map.of("key", "value"),
              map,
              "Unexpected received message"
          );
          return true;
        })
    );
  }

  @Test
  void shouldPutMsisdnToMessage() {
    redisTemplate.opsForHash().put("cookie-to-msisdn", "cookie-value", "msisdn-value");

    rabbitTemplate.send(inputQueue, newMessage(
        Map.of(
            "key", "value",
            "cookie", "cookie-value"
        )
    ));

    verify(messageConsumer, timeout(5000).times(1)).accept(
        argThat(received -> {
          Map<String, Object> map = jsonToMap(received);
          assertEquals(
              Map.of(
                  "key", "value",
                  "cookie", "cookie-value",
                  "msisdn", "msisdn-value"
              ),
              map,
              "Unexpected received message"
          );
          return true;
        })
    );
  }

  @Test
  void shouldSaveCookieMsisdnMapping() {
    rabbitTemplate.send(inputQueue, newMessage(
        Map.of(
            "key", "value",
            "cookie", "cookie-value",
            "msisdn", "msisdn-value"
        )
    ));

    verify(messageConsumer, timeout(5000).times(1)).accept(
        argThat(received -> {
          Map<String, Object> map = jsonToMap(received);
          assertEquals(
              Map.of(
                  "key", "value",
                  "cookie", "cookie-value",
                  "msisdn", "msisdn-value"
              ),
              map,
              "Unexpected received message"
          );
          return true;
        })
    );

    String msisdn = (String) redisTemplate.opsForHash().get("cookie-to-msisdn", "cookie-value");
    assertEquals("msisdn-value", msisdn, "Unexpected msisdn value");
  }

  @SneakyThrows
  private Message newMessage(Map<?, ?> content) {
    return new Message(
        objectMapper.writeValueAsString(content)
            .getBytes(UTF_8)
    );
  }

  @SneakyThrows
  @SuppressWarnings("unchecked")
  private Map<String, Object> jsonToMap(String json) {
    return objectMapper.readValue(json, Map.class);
  }
}