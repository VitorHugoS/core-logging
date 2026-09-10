package com.corelogging;

import org.slf4j.LoggerFactory;
import tools.jackson.databind.ObjectMapper;

public class CoreLoggerFactory {

  private final ObjectMapper objectMapper;

  public CoreLoggerFactory(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public CoreLogger getLogger(Class<?> clazz) {
    return new CoreLogger(LoggerFactory.getLogger(clazz), objectMapper);
  }

  public CoreLogger getLogger(String name) {
    return new CoreLogger(LoggerFactory.getLogger(name), objectMapper);
  }
}
