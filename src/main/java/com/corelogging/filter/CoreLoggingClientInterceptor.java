package com.corelogging.filter;

import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

public class CoreLoggingClientInterceptor implements ClientHttpRequestInterceptor {

  private static final Logger log = LoggerFactory.getLogger(CoreLoggingClientInterceptor.class);

  @Override
  public ClientHttpResponse intercept(
      HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {

    try (var scope = com.corelogging.scope.ObservabilityScope.start(log, "out_request", "CLIENT")) {
      if (request.getMethod() != null) {
        scope.tag("http.method", request.getMethod().name());
      }
      scope.tag("http.url", request.getURI().toString());

      ClientHttpResponse response = null;
      try {
        response = execution.execute(request, body);
        return response;
      } catch (Throwable t) {
        scope.recordError(t);
        throw t;
      } finally {
        scope.computeDurationAs("http.duration_ms");
        if (response != null) {
          scope.tag("http.status_code", String.valueOf(response.getStatusCode().value()));
        }

        if (scope.hasError()) {
          scope.closeWith("Failed outgoing request {} {}", request.getMethod(), request.getURI());
        } else {
          scope.closeWith(
              "Processed outgoing request {} {}", request.getMethod(), request.getURI());
        }
      }
    }
  }
}
