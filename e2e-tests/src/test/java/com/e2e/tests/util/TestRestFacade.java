package com.e2e.tests.util;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.HttpMethod.POST;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;

@TestComponent
public class TestRestFacade {

  @Value("${api.exposed-port}")
  private int port;
  @Value("${api.host}")
  private String host;
  @Autowired
  private TestRestTemplate rest;

  public <T> ResponseEntity<T> post(String url, Object body, Class<T> responseType) {
    final var response = rest.exchange(
        "http://" + host + ":" + port + url,
        POST,
        new HttpEntity<>(body),
        responseType
    );
    assertTrue(
        response.getStatusCode().is2xxSuccessful(),
        "Unexpected status code: " + response.getStatusCode()
    );
    return response;
  }
}
