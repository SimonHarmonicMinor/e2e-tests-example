package com.e2e.api;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class APIController {
  private final MessageSender messageSender;

  @PostMapping("/api/message")
  public void sendMessage(@RequestBody Map<String, Object> message) {
    messageSender.sendMessage(message);
  }
}
