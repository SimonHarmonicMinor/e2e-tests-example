package com.e2e.tests;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.testcontainers.shaded.org.awaitility.Durations.FIVE_SECONDS;

import com.e2e.tests.util.E2ESuite;
import java.util.Map;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

class GainTest extends E2ESuite {

  @Test
  @SneakyThrows
  void shouldGainMessage() {
    rest.post(
        "/api/message",
        Map.of(
            "some_key", "some_value",
            "cookie", "cookie-value",
            "msisdn", "88005553535"
        ),
        Void.class
    );
    await().atMost(FIVE_SECONDS)
        .until(() -> getGainQueueMessages().contains(Map.of(
            "some_key", "some_value",
            "cookie", "cookie-value",
            "msisdn", "88005553535"
        )));

    Thread.sleep(5000);
    assertTrue(getGainQueueMessages().contains(Map.of(
        "some_key", "some_value",
        "cookie", "cookie-value",
        "msisdn", "88005553535"
    )));

    rest.post(
        "/api/message",
        Map.of(
            "another_key", "another_value",
            "cookie", "cookie-value"
        ),
        Void.class
    );
    await().atMost(FIVE_SECONDS)
        .until(() -> getGainQueueMessages().contains(Map.of(
            "another_key", "another_value",
            "cookie", "cookie-value",
            "msisdn", "88005553535"
        )));
  }
}
