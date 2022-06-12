package com.e2e.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import com.e2e.api.testutils.FunctionalTestSuite;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.function.Consumer;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

@Import(TestRabbitListener.class)
class APIControllerFunctionalTest extends FunctionalTestSuite {
  @Autowired
  private TestRestTemplate rest;
  @Autowired
  private Consumer<String> messageConsumer;
  @Autowired
  private ObjectMapper objectMapper;

  @Test
  void shouldForwardMessageToQueue() {
    ResponseEntity<Void> response = rest.exchange(
        "/api/message",
        HttpMethod.POST,
        new HttpEntity<>(Map.of("field", "value")),
        Void.class
    );

    assertTrue(response.getStatusCode().is2xxSuccessful(), "Unexpected response status: " + response.getStatusCode());

    verify(messageConsumer, timeout(5000).times(1)).accept(
        argThat(str -> {
          Map<String, Object> content = parseJSON(str);
          assertEquals(
              Map.of("field", "value"),
              content,
              "Unexpected received message"
          );
          return true;
        })
    );
  }

  @SneakyThrows
  @SuppressWarnings("unchecked")
  private Map<String, Object> parseJSON(String str) {
    return objectMapper.readValue(str, Map.class);
  }
}