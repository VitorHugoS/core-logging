package com.corelogging.filter;

import feign.Capability;
import feign.Client;
import feign.Request;
import feign.Response;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

public class CoreLoggingFeignCapability implements Capability {
  private static final Logger log = LoggerFactory.getLogger(CoreLoggingFeignCapability.class);

  @Override
  public Client enrich(Client client) {
    return (request, options) -> {
      long startTime = System.currentTimeMillis();
      try {
        Response response = client.execute(request, options);
        long duration = System.currentTimeMillis() - startTime;
        logFeignRequest(request, response.status(), duration);
        return response;
      } catch (IOException e) {
        long duration = System.currentTimeMillis() - startTime;
        logFeignException(request, e, duration);
        throw e;
      }
    };
  }

  private void logFeignRequest(Request request, int status, long duration) {
    try {
      MDC.put("log_type", "out_request");
      MDC.put("span.kind", "CLIENT");
      MDC.put("http.method", request.httpMethod().name());
      MDC.put("http.url", request.url());
      MDC.put("http.status_code", String.valueOf(status));
      MDC.put("http.duration_ms", String.valueOf(duration));

      log.info("Outgoing Feign request to {}", request.url());
    } finally {
      MDC.remove("log_type");
      MDC.remove("span.kind");
      MDC.remove("http.method");
      MDC.remove("http.url");
      MDC.remove("http.status_code");
      MDC.remove("http.duration_ms");
    }
  }

  private void logFeignException(Request request, IOException e, long duration) {
    try {
      MDC.put("log_type", "out_request");
      MDC.put("span.kind", "CLIENT");
      MDC.put("http.method", request.httpMethod().name());
      MDC.put("http.url", request.url());
      MDC.put("http.duration_ms", String.valueOf(duration));

      java.io.StringWriter sw = new java.io.StringWriter();
      e.printStackTrace(new java.io.PrintWriter(sw));
      MDC.put("error.stacktrace", sw.toString());

      log.error("Feign request failed to {}: {}", request.url(), e.getMessage(), e);
    } finally {
      MDC.remove("log_type");
      MDC.remove("span.kind");
      MDC.remove("http.method");
      MDC.remove("http.url");
      MDC.remove("http.duration_ms");
      MDC.remove("error.stacktrace");
    }
  }
}
