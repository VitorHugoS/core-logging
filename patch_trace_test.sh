#!/bin/bash
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
sed -i '' -e '$d' src/test/java/com/corelogging/filter/CoreLoggingFilterTest.java
cat patch_trace_test.sh | grep -v '#!/bin/bash' | grep -v 'cat <<' | grep -v 'INNER_EOF' | grep -v 'sed' | grep -v 'cat patch_trace_test.sh' >> src/test/java/com/corelogging/filter/CoreLoggingFilterTest.java

cat << 'INNER_EOF2' >> src/test/java/com/corelogging/filter/CoreLoggingKafkaConsumerInterceptorTest.java

  @Test
  void shouldFallbackToTraceIdWhenHeaderIsMissing() {
    MDC.put("traceId", "my-otel-trace-id");
    when(record.headers()).thenReturn(new org.apache.kafka.common.header.internals.RecordHeaders());
    
    interceptor.intercept(record, consumer);

    assertThat(MDC.get("correlation_id")).isEqualTo("my-otel-trace-id");
  }
}
INNER_EOF2
sed -i '' -e '$d' src/test/java/com/corelogging/filter/CoreLoggingKafkaConsumerInterceptorTest.java
cat patch_trace_test.sh | grep -v '#!/bin/bash' | grep -v 'cat <<' | grep -v 'INNER_EOF' | grep -v 'sed' | grep -v 'cat patch_trace_test.sh' >> src/test/java/com/corelogging/filter/CoreLoggingKafkaConsumerInterceptorTest.java
