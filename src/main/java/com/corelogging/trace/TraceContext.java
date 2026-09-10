package com.corelogging.trace;

public interface TraceContext {
  void tag(String key, String value);

  void recordError(Throwable t);

  boolean hasError();
}
