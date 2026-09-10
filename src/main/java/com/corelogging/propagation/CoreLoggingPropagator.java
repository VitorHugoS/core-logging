package com.corelogging.propagation;

import com.corelogging.config.CoreLoggingProperties;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.propagation.Propagator;
import java.util.List;
import java.util.UUID;
import org.slf4j.MDC;

public class CoreLoggingPropagator implements Propagator {

  private final CoreLoggingProperties properties;

  public CoreLoggingPropagator(CoreLoggingProperties properties) {
    this.properties = properties;
  }

  @Override
  public List<String> fields() {
    return properties.getAcceptedCorrelationIdHeaders();
  }

  @Override
  public <C> void inject(TraceContext context, C carrier, Setter<C> setter) {
    String correlationId = MDC.get("correlation_id");
    if (correlationId != null) {
      setter.set(carrier, properties.getCorrelationIdHeader(), correlationId);
    }
  }

  @Override
  public <C> Span.Builder extract(C carrier, Getter<C> getter) {
    // To avoid MDC leaks, we don't put it in MDC here. The caller should tag it in the
    // TraceContext.
    return Span.Builder.NOOP;
  }

  public <C> String extractId(C carrier, Getter<C> getter) {
    String correlationId = null;
    for (String headerName : properties.getAcceptedCorrelationIdHeaders()) {
      String val = getter.get(carrier, headerName);
      if (val != null && !val.trim().isEmpty()) {
        correlationId = val;
        break;
      }
    }

    if (correlationId == null) {
      correlationId = MDC.get("traceId");
    }

    if (correlationId == null) {
      correlationId = UUID.randomUUID().toString();
    }
    return correlationId;
  }
}
