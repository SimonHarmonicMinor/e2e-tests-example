package com.e2e.tests.util;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import com.e2e.tests.util.E2ESuite.Initializer;
import com.e2e.tests.util.TestRabbitListener.QueueType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.lifecycle.Startables;

@ContextConfiguration(initializers = Initializer.class)
@SpringBootTest(webEnvironment = RANDOM_PORT)
@Import({
    TestRedisFacade.class,
    TestRabbitListener.class,
    TestRestFacade.class
})
public class E2ESuite {

  private static final Network SHARED_NETWORK = Network.newNetwork();
  private static final GenericContainer<?> REDIS =
      new GenericContainer<>("redis:5.0.14-alpine3.15")
          .withExposedPorts(6379)
          .withNetwork(SHARED_NETWORK)
          .withNetworkAliases("redis")
          .withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger("Redis")));
  private static RabbitMQContainer RABBIT;

  private static GenericContainer<?> API_SERVICE;
  private static GenericContainer<?> GAIN_SERVICE;

  @Autowired
  protected TestRedisFacade redis;
  @Autowired
  protected TestRestFacade rest;
  @Autowired
  private TestRabbitListener testRabbitListener;
  @Autowired
  private ObjectMapper objectMapper;

  @BeforeEach
  void beforeEach() {
    redis.cleanRedis();
    testRabbitListener.resetMessages();
  }

  protected List<Map<String, ?>> getGainQueueMessages() {
    return toJSONList(testRabbitListener.getMessages().get(QueueType.GAIN));
  }

  @SuppressWarnings("unchecked")
  @SneakyThrows
  private List<Map<String, ?>> toJSONList(List<String> list) {
    final var result = new ArrayList<Map<String, ?>>();
    for (String item : list) {
      result.add(objectMapper.readValue(item, Map.class));
    }
    return result;
  }

  static class Initializer implements
      ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext context) {
      final var environment = context.getEnvironment();
      RABBIT = new RabbitMQContainer("rabbitmq:3.7.25-management-alpine")
          .withNetwork(SHARED_NETWORK)
          .withNetworkAliases("rabbit")
          .withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger("Rabbit")))
          .withQueue(
              requireNonNull(
                  environment.getProperty("queue.api", String.class),
                  "API Queue is null"
              )
          )
          .withQueue(
              requireNonNull(
                  environment.getProperty("queue.gain", String.class),
                  "Gain Queue is null"
              )
          )
          .withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger("Rabbit")));

      Startables.deepStart(REDIS, RABBIT).join();
      final var apiExposedPort = requireNonNull(
          environment.getProperty("api.exposed-port", Integer.class),
          "API Exposed Port is null"
      );
      API_SERVICE = createApiServiceContainer(environment, apiExposedPort);
      GAIN_SERVICE = createGainServiceContainer(environment);
      Startables.deepStart(API_SERVICE, GAIN_SERVICE).join();

      environment.getPropertySources().addFirst(
          new MapPropertySource(
              "testcontainers",
              Map.of(
                  "spring.rabbitmq.addresses", RABBIT.getAmqpUrl(),
                  "spring.redis.url", format(
                      "redis://%s:%s",
                      REDIS.getHost(),
                      REDIS.getMappedPort(6379)
                  ),
                  "api.host", API_SERVICE.getHost()
              )
          )
      );
    }

    private GenericContainer<?> createApiServiceContainer(
        Environment environment,
        int apiExposedPort
    ) {
      final var apiServiceImage = requireNonNull(
          environment.getProperty(
              "image.api-service",
              String.class
          ),
          "API-Service image is null"
      );
      final var queue = requireNonNull(
          environment.getProperty(
              "queue.api",
              String.class
          ),
          "API Queue is null"
      );
      return new GenericContainer<>(apiServiceImage)
          .withEnv("SPRING_RABBITMQ_ADDRESSES", "amqp://rabbit:5672")
          .withEnv("QUEUE_NAME", queue)
          .withExposedPorts(8080)
          .withNetwork(SHARED_NETWORK)
          .withNetworkAliases("api-service")
          .withCreateContainerCmdModifier(
              cmd -> cmd.withHostConfig(
                  new HostConfig()
                      .withNetworkMode(SHARED_NETWORK.getId())
                      .withPortBindings(new PortBinding(
                          Ports.Binding.bindPort(apiExposedPort),
                          new ExposedPort(8080)
                      ))
              )
          )
          .waitingFor(
              Wait.forHttp("/actuator/health")
                  .forStatusCode(200)
          )
          .withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger("API-Service")));
    }

    private GenericContainer<?> createGainServiceContainer(Environment environment) {
      final var gainServiceImage = requireNonNull(
          environment.getProperty(
              "image.gain-service",
              String.class
          ),
          "Gain-Service image is null"
      );
      final var apiQueue = requireNonNull(
          environment.getProperty(
              "queue.api",
              String.class
          ),
          "API Queue is null"
      );
      final var gainQueue = requireNonNull(
          environment.getProperty(
              "queue.gain",
              String.class
          ),
          "Gain Queue is null"
      );
      return new GenericContainer<>(gainServiceImage)
          .withNetwork(SHARED_NETWORK)
          .withNetworkAliases("gain-service")
          .withEnv("SPRING_RABBITMQ_ADDRESSES", "amqp://rabbit:5672")
          .withEnv("SPRING_REDIS_URL", "redis://redis:6379")
          .withEnv("QUEUE_INPUT_NAME", apiQueue)
          .withEnv("QUEUE_OUTPUT_NAME", gainQueue)
          .waitingFor(
              Wait.forHttp("/actuator/health")
                  .forStatusCode(200)
          )
          .withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger("Gain-Service")));
    }
  }
}
