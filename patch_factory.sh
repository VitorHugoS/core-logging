#!/bin/bash
cat << 'INNER_EOF' > src/main/java/com/corelogging/CoreLoggerFactory.java
package com.corelogging;

import com.corelogging.config.CoreLoggingProperties;
import java.util.List;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.ObjectMapper;

public class CoreLoggerFactory {

  private final ObjectMapper objectMapper;
  private final List<String> obfuscateFields;

  public CoreLoggerFactory(ObjectMapper objectMapper, CoreLoggingProperties properties) {
    this.objectMapper = objectMapper;
    this.obfuscateFields = properties != null ? properties.getPayload().getObfuscateFields() : List.of();
  }

  public CoreLogger getLogger(Class<?> clazz) {
    return new CoreLogger(LoggerFactory.getLogger(clazz), objectMapper, obfuscateFields);
  }

  public CoreLogger getLogger(String name) {
    return new CoreLogger(LoggerFactory.getLogger(name), objectMapper, obfuscateFields);
  }
}
INNER_EOF
