package com.corelogging;

import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.MDC;
import tools.jackson.databind.ObjectMapper;

public class CoreLogger {

  private final Logger logger;
  private final ObjectMapper objectMapper;

  public CoreLogger(Logger logger, ObjectMapper objectMapper) {
    this.logger = logger;
    this.objectMapper = objectMapper;
  }

  public LogBuilder info(String format, Object... args) {
    return new LogBuilder(logger, LogLevel.INFO, format, args, objectMapper);
  }

  public LogBuilder error(String format, Object... args) {
    return new LogBuilder(logger, LogLevel.ERROR, format, args, objectMapper);
  }

  public LogBuilder warn(String format, Object... args) {
    return new LogBuilder(logger, LogLevel.WARN, format, args, objectMapper);
  }

  public LogBuilder debug(String format, Object... args) {
    return new LogBuilder(logger, LogLevel.DEBUG, format, args, objectMapper);
  }

  public enum LogLevel {
    INFO,
    ERROR,
    WARN,
    DEBUG
  }

  public static class LogBuilder {
    private final Logger logger;
    private final LogLevel level;
    private final String format;
    private final Object[] args;
    private final ObjectMapper objectMapper;
    private final Map<String, String> customFields = new HashMap<>();

    LogBuilder(
        Logger logger, LogLevel level, String format, Object[] args, ObjectMapper objectMapper) {
      this.logger = logger;
      this.level = level;
      this.format = format;
      this.args = args;
      this.objectMapper = objectMapper;
    }

    public LogBuilder with(String key, String value) {
      if (key != null && value != null) {
        customFields.put(key, value);
      }
      return this;
    }

    public LogBuilder with(String key, Object value) {
      if (key != null && value != null) {
        customFields.put(key, serializeValue(value));
      }
      return this;
    }

    public LogBuilder with(Object payload) {
      if (payload != null) {
        try {
          Map<String, Object> map =
              objectMapper.convertValue(
                  payload, new tools.jackson.core.type.TypeReference<Map<String, Object>>() {});
          if (map != null) {
            map.forEach(
                (k, v) -> {
                  if (v != null) {
                    customFields.put(k, serializeValue(v));
                  }
                });
          }
        } catch (Exception e) {
          // Ignore exceptions and skip flattening
        }
      }
      return this;
    }

    private String serializeValue(Object value) {
      if (value instanceof String) {
        return (String) value;
      }
      try {
        return objectMapper.writeValueAsString(value);
      } catch (Exception e) {
        return String.valueOf(value);
      }
    }

    public void log() {
      if (!isLevelEnabled()) {
        return;
      }

      String previousLogType = MDC.get("log_type");

      try {
        MDC.put("log_type", "application");
        customFields.forEach(MDC::put);
        executeLog();
      } finally {
        if (previousLogType != null) {
          MDC.put("log_type", previousLogType);
        } else {
          MDC.remove("log_type");
        }
        customFields.keySet().forEach(MDC::remove);
      }
    }

    private boolean isLevelEnabled() {
      return switch (level) {
        case INFO -> logger.isInfoEnabled();
        case ERROR -> logger.isErrorEnabled();
        case WARN -> logger.isWarnEnabled();
        case DEBUG -> logger.isDebugEnabled();
      };
    }

    private void executeLog() {
      boolean hasArgs = args != null && args.length != 0;
      switch (level) {
        case INFO -> {
          if (hasArgs) logger.info(format, args);
          else logger.info(format);
        }
        case ERROR -> {
          if (hasArgs) logger.error(format, args);
          else logger.error(format);
        }
        case WARN -> {
          if (hasArgs) logger.warn(format, args);
          else logger.warn(format);
        }
        case DEBUG -> {
          if (hasArgs) logger.debug(format, args);
          else logger.debug(format);
        }
      }
    }
  }
}
