package com.corelogging.filter;

import com.corelogging.auditor.DefaultHttpPayloadAuditor;
import com.corelogging.auditor.HttpPayloadAuditor;
import com.corelogging.config.CoreLoggingProperties;
import com.corelogging.propagation.CoreLoggingPropagator;
import com.corelogging.trace.DefaultTraceManager;
import com.corelogging.trace.TraceContext;
import com.corelogging.trace.TraceManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.core.Ordered;
import org.springframework.web.filter.OncePerRequestFilter;

public class CoreLoggingFilter extends OncePerRequestFilter implements Ordered {

  private final CoreLoggingProperties properties;
  private final TraceManager traceManager;
  private final HttpPayloadAuditor payloadAuditor;
  private final CoreLoggingPropagator propagator;

  public CoreLoggingFilter(CoreLoggingProperties properties) {
    this.properties = properties;
    this.traceManager = new DefaultTraceManager();
    this.payloadAuditor = new DefaultHttpPayloadAuditor(properties);
    this.propagator = new CoreLoggingPropagator(properties);
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    try {
      traceManager.observe(
          "in_request",
          "SERVER",
          "processing incoming request " + request.getMethod() + " " + request.getRequestURI(),
          context -> {
            context.tag("http.method", request.getMethod());
            context.tag("http.url", request.getRequestURI());

            // Extract Correlation ID
            String extractedId = propagator.extractId(request, HttpServletRequest::getHeader);
            context.tag("correlation_id", extractedId);

            // Inject it into response just for the client
            String correlationId = org.slf4j.MDC.get("correlation_id");
            if (correlationId != null) {
              response.setHeader(properties.getCorrelationIdHeader(), correlationId);
            }

            try {
              payloadAuditor.auditAndProceed(request, response, filterChain);
            } catch (Throwable t) {
              context.recordError(t);
              if (t instanceof Exception) throw (Exception) t;
              throw new RuntimeException(t);
            } finally {
              applyStatusCode(response, context);
            }
          });
    } catch (ServletException | IOException e) {
      throw e;
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void applyStatusCode(HttpServletResponse response, TraceContext context) {
    int status = response.getStatus();
    if (context.hasError() && status == 200) {
      status = 500;
    }
    context.tag("http.status_code", String.valueOf(status));
  }

  @Override
  public int getOrder() {
    return Ordered.LOWEST_PRECEDENCE - 10;
  }
}
