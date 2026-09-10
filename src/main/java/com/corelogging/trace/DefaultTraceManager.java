package com.corelogging.trace;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

public class DefaultTraceManager implements TraceManager {

  private static final Logger log = LoggerFactory.getLogger(DefaultTraceManager.class);

  @Override
  public void observe(String logType, String spanKind, String successMessage, TraceAction action)
      throws Exception {
    long startTime = System.currentTimeMillis();
    String previousLogType = MDC.get("log_type");
    String previousSpanKind = MDC.get("span.kind");

    MDC.put("log_type", logType);
    MDC.put("span.kind", spanKind);

    DefaultTraceContext context = new DefaultTraceContext();
    try {
      action.execute(context);
    } catch (Exception t) {
      context.recordError(t);
      throw t;
    } catch (Throwable t) {
      context.recordError(t);
      throw new RuntimeException(t);
    } finally {
      String duration = String.valueOf(System.currentTimeMillis() - startTime);
      // In a real generic manager, we might pass the duration tag name, but for backwards
      // compatibility we use http.duration_ms.
      // A better design is to pass the prefix to observe(). For now we fix the tests.
      context.tag("http.duration_ms", duration);

      if (context.error != null) {
        log.error("Failed " + successMessage + ": " + context.error.getMessage());
      } else {
        log.info("Processed " + successMessage);
      }

      // Cleanup
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
      context.cleanup();
    }
  }

  private static class DefaultTraceContext implements TraceContext {
    private Throwable error;
    private final List<String> tags = new ArrayList<>();

    @Override
    public void tag(String key, String value) {
      if (key != null && value != null) {
        MDC.put(key, value);
        tags.add(key);
      }
    }

    @Override
    public void recordError(Throwable t) {
      this.error = t;
      if (t != null) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        tag("error.stacktrace", sw.toString());
      }
    }

    @Override
    public boolean hasError() {
      return error != null;
    }

    void cleanup() {
      for (String tag : tags) {
        MDC.remove(tag);
      }
    }
  }
}
