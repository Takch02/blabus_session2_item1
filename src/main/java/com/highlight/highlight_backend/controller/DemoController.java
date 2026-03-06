package com.highlight.highlight_backend.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class DemoController {

  @GetMapping("/ping")
  public String ping() {
    log.info("ping called");
    return "pong";
  }

  @GetMapping("/error-demo")
  public String errorDemo() {
    try {
      throw new RuntimeException("demo exception");
    } catch (Exception e) {
      log.error("error-demo triggered", e);
      return "error logged";
    }
  }
}
