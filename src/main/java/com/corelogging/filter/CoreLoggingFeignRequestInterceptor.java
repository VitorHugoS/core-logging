package com.corelogging.filter;

import com.corelogging.config.CoreLoggingProperties;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.slf4j.MDC;

public class CoreLoggingFeignRequestInterceptor implements RequestInterceptor {

  private final CoreLoggingProperties properties;

  public CoreLoggingFeignRequestInterceptor(CoreLoggingProperties properties) {
    this.properties = properties;
  }

  @Override
  public void apply(RequestTemplate template) {
    String correlationId = MDC.get("correlation_id");
    if (correlationId != null) {
      template.header(properties.getCorrelationIdHeader(), correlationId);
    }
  }
}
