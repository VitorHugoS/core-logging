package com.corelogging.filter;

import com.corelogging.config.CoreLoggingProperties;
import com.corelogging.propagation.CoreLoggingPropagator;
import com.corelogging.trace.DefaultTraceManager;
import com.corelogging.trace.TraceManager;
import java.io.IOException;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

public class CoreLoggingClientInterceptor implements ClientHttpRequestInterceptor {

  private final TraceManager traceManager;
  private final CoreLoggingPropagator propagator;

  public CoreLoggingClientInterceptor(CoreLoggingProperties properties) {
    this.traceManager = new DefaultTraceManager();
    this.propagator = new CoreLoggingPropagator(properties);
  }

  @Override
  public ClientHttpResponse intercept(
      HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {

    final java.util.concurrent.atomic.AtomicReference<ClientHttpResponse> responseRef =
        new java.util.concurrent.atomic.AtomicReference<>();

    try {
      traceManager.observe(
          "out_request",
          "CLIENT",
          "outgoing request " + request.getMethod() + " " + request.getURI(),
          context -> {
            if (request.getMethod() != null) {
              context.tag("http.method", request.getMethod().name());
            }
            context.tag("http.url", request.getURI().toString());

            propagator.inject(
                null, request, (carrier, key, value) -> carrier.getHeaders().add(key, value));

            ClientHttpResponse response = execution.execute(request, body);
            responseRef.set(response);
            if (response != null) {
              try {
                context.tag("http.status_code", String.valueOf(response.getStatusCode().value()));
              } catch (IOException e) {
                // Ignore
              }
            }
          });
      return responseRef.get();
    } catch (IOException | RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
