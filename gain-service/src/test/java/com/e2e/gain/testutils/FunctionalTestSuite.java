package com.e2e.gain.testutils;

import static java.lang.String.format;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import com.e2e.gain.testutils.FunctionalTestSuite.Initializer;
import java.util.Map;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.lifecycle.Startables;
import org.testcontainers.utility.DockerImageName;

@ContextConfiguration(initializers = Initializer.class)
@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
public class FunctionalTestSuite {

  private static final GenericContainer<?> REDIS =
      new GenericContainer<>("redis:5.0.14-alpine3.15")
          .withExposedPorts(6379);
  private static final RabbitMQContainer RABBIT =
      new RabbitMQContainer(DockerImageName.parse("rabbitmq:3.7.25-management-alpine"));

  static class Initializer implements
      ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
      Startables.deepStart(REDIS, RABBIT).join();

      ConfigurableEnvironment environment = applicationContext.getEnvironment();
      environment.getPropertySources().addFirst(
          new MapPropertySource(
              "testcontainers",
              Map.of(
                  "spring.redis.url",
                  format("redis://%s:%s", REDIS.getHost(), REDIS.getMappedPort(6379)),
                  "spring.rabbitmq.addresses", RABBIT.getAmqpUrl()
              )
          )
      );
    }
  }

}
