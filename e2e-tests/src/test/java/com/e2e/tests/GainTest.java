package com.e2e.tests;

import static org.awaitility.Awaitility.await;
import static org.testcontainers.shaded.org.awaitility.Durations.FIVE_SECONDS;

import com.e2e.tests.util.E2ESuite;
import java.util.Map;
import org.junit.jupiter.api.Test;

class GainTest extends E2ESuite {

  @Test
  void shouldGainMessage() {
    rest.post(
        "/api/message",
        Map.of(
            "some_key", "some_value",
            "cookie", "cookie-value",
            "msisdn", "msisdn-value"
        ),
        Void.class
    );
    await().atMost(FIVE_SECONDS)
        .until(() -> getGainQueueMessages().contains(Map.of(
            "some_key", "some_value",
            "cookie", "cookie-value",
            "msisdn", "msisdn-value"
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
            "msisdn", "msisdn-value"
        )));
  }
}
