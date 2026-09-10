#!/bin/bash
sed -i '' -e '$d' src/test/java/com/corelogging/filter/CoreLoggingFilterTest.java
cat << 'INNER_EOF' >> src/test/java/com/corelogging/filter/CoreLoggingFilterTest.java

  @Test
  void shouldFallbackToTraceIdWhenHeaderIsMissing() throws Exception {
    request.setMethod("GET");
    request.setRequestURI("/test");
    MDC.put("traceId", "my-otel-trace-id");

    filter.doFilter(request, response, filterChain);

    assertThat(response.getHeader("x-correlation-id")).isEqualTo("my-otel-trace-id");
    assertThat(TestAppender.events).isNotEmpty();
    assertThat(TestAppender.events.get(0).getMDCPropertyMap().get("correlation_id"))
        .isEqualTo("my-otel-trace-id");
  }
}
INNER_EOF
