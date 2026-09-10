package com.corelogging.trace;

public interface TraceManager {

  /**
   * Executes the given action within a managed trace context. Handles MDC injection, duration
   * calculation, error catching, and final log emission.
   */
  void observe(String logType, String spanKind, String successMessage, TraceAction action)
      throws Exception;
}
