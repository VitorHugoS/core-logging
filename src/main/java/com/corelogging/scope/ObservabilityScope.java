package com.corelogging.scope;

import java.io.PrintWriter;
import java.io.StringWriter;
import org.slf4j.Logger;
import org.slf4j.MDC;

public class ObservabilityScope implements AutoCloseable {

  private final Logger logger;
  private final long startTime;
  private final String previousLogType;
  private final String previousSpanKind;
  private final java.util.List<String> tags = new java.util.ArrayList<>();

  private Throwable error;

  private ObservabilityScope(Logger logger, String logType, String spanKind) {
    this.logger = logger;
    this.startTime = System.currentTimeMillis();
    this.previousLogType = MDC.get("log_type");
    this.previousSpanKind = MDC.get("span.kind");

    MDC.put("log_type", logType);
    MDC.put("span.kind", spanKind);
  }

  public static ObservabilityScope start(Logger logger, String logType, String spanKind) {
    return new ObservabilityScope(logger, logType, spanKind);
  }

  public ObservabilityScope tag(String key, String value) {
    if (key != null && value != null) {
      MDC.put(key, value);
      tags.add(key);
    }
    return this;
  }

  public ObservabilityScope recordError(Throwable t) {
    this.error = t;
    if (t != null) {
      StringWriter sw = new StringWriter();
      t.printStackTrace(new PrintWriter(sw));
      MDC.put("error.stacktrace", sw.toString());
      tags.add("error.stacktrace");
    }
    return this;
  }

  public boolean hasError() {
    return this.error != null;
  }

  public void closeWith(String message, Object... args) {
    try {
      if (error != null) {
        logger.error(message, args);
      } else {
        logger.info(message, args);
      }
    } finally {
      close();
    }
  }

  public void computeDurationAs(String key) {
    String duration = String.valueOf(System.currentTimeMillis() - startTime);
    MDC.put(key, duration);
    tags.add(key);
  }

  @Override
  public void close() {
    if (previousLogType != null) {
      MDC.put("log_type", previousLogType);
    } else {
      MDC.remove("log_type");
    }

    if (previousSpanKind != null) {
      MDC.put("span.kind", previousSpanKind);
    } else {
      MDC.remove("span.kind");
    }

    for (String tag : tags) {
      MDC.remove(tag);
    }
  }
}
