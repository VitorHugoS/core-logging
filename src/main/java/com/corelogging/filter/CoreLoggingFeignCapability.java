package com.corelogging.filter;

import feign.Capability;
import feign.Client;
import feign.Response;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CoreLoggingFeignCapability implements Capability {
  private static final Logger log = LoggerFactory.getLogger(CoreLoggingFeignCapability.class);

  @Override
  public Client enrich(Client client) {
    return (request, options) -> {
      try (var scope =
          com.corelogging.scope.ObservabilityScope.start(log, "out_request", "CLIENT")) {
        scope.tag("http.method", request.httpMethod().name());
        scope.tag("http.url", request.url());

        try {
          Response response = client.execute(request, options);
          scope.tag("http.status_code", String.valueOf(response.status()));
          scope.computeDurationAs("http.duration_ms");
          scope.closeWith("Processed outgoing Feign request to {}", request.url());
          return response;
        } catch (IOException e) {
          scope.computeDurationAs("http.duration_ms");
          scope.recordError(e);
          scope.closeWith("Failed outgoing Feign request to {}", request.url());
          throw e;
        }
      }
    };
  }
}
