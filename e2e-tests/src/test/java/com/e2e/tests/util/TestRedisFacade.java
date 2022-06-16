package com.e2e.tests.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.data.redis.core.RedisTemplate;

@TestComponent
public class TestRedisFacade {
  @Autowired
  private RedisTemplate<String, String> redisTemplate;

  public void cleanRedis() {
    redisTemplate.delete("cookie-to-msisdn");
  }

  public RedisTemplate<String, String> client() {
    return redisTemplate;
  }
}
