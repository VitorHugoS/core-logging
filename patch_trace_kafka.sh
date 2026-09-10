#!/bin/bash
sed -i '' -e '$d' src/test/java/com/corelogging/filter/CoreLoggingKafkaConsumerInterceptorTest.java
cat << 'INNER_EOF' >> src/test/java/com/corelogging/filter/CoreLoggingKafkaConsumerInterceptorTest.java

  @Test
  void shouldFallbackToTraceIdWhenHeaderIsMissing() {
    MDC.put("traceId", "my-otel-trace-id");
    when(record.headers()).thenReturn(new org.apache.kafka.common.header.internals.RecordHeaders());
    
    interceptor.intercept(record, consumer);

    assertThat(MDC.get("correlation_id")).isEqualTo("my-otel-trace-id");
  }
}
INNER_EOF
