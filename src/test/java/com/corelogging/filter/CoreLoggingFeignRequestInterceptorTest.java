package com.corelogging.filter;

import static org.assertj.core.api.Assertions.assertThat;

import com.corelogging.config.CoreLoggingProperties;
import feign.RequestTemplate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

class CoreLoggingFeignRequestInterceptorTest {

  @AfterEach
  void tearDown() {
    MDC.clear();
  }

  @Test
  void shouldAddCorrelationIdHeaderIfPresentInMdc() {
    CoreLoggingProperties properties = new CoreLoggingProperties();
    CoreLoggingFeignRequestInterceptor interceptor =
        new CoreLoggingFeignRequestInterceptor(properties);
    RequestTemplate template = new RequestTemplate();

    MDC.put("correlation_id", "12345");
    interceptor.apply(template);

    assertThat(template.headers().get("x-correlation-id")).containsExactly("12345");
  }

  @Test
  void shouldNotAddCorrelationIdHeaderIfNotPresentInMdc() {
    CoreLoggingProperties properties = new CoreLoggingProperties();
    CoreLoggingFeignRequestInterceptor interceptor =
        new CoreLoggingFeignRequestInterceptor(properties);
    RequestTemplate template = new RequestTemplate();

    interceptor.apply(template);

    assertThat(template.headers()).isEmpty();
  }
}
