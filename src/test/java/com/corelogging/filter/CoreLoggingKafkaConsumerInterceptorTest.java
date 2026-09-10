package com.corelogging.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

class CoreLoggingKafkaConsumerInterceptorTest {

  private CoreLoggingKafkaConsumerInterceptor<Object, Object> interceptor;
  private Consumer<Object, Object> consumer;
  private ConsumerRecord<Object, Object> record;

  @BeforeEach
  @SuppressWarnings("unchecked")
  void setUp() {
    interceptor = new CoreLoggingKafkaConsumerInterceptor<>();
    consumer = mock(Consumer.class);
    record = mock(ConsumerRecord.class);
    when(record.topic()).thenReturn("test-topic");
    when(record.headers()).thenReturn(new RecordHeaders());

    MDC.clear();
    TestAppender.clear();
  }

  @Test
  void shouldExtractCorrelationIdIfPresent() {
    RecordHeaders headers = new RecordHeaders();
    headers.add("correlation_id", "12345".getBytes(StandardCharsets.UTF_8));
    when(record.headers()).thenReturn(headers);
    when(record.topic()).thenReturn("test-topic");
    when(record.value()).thenReturn("test-payload");

    interceptor.intercept(record, consumer);

    assertThat(MDC.get("correlation_id")).isEqualTo("12345");
  }

  @Test
  void shouldLogAndPopulateMdcOnSuccess() {
    ConsumerRecord<Object, Object> returned = interceptor.intercept(record, consumer);
    assertThat(returned).isEqualTo(record);

    assertThat(MDC.get("log_type")).isEqualTo("in_message");
    assertThat(MDC.get("span.kind")).isEqualTo("CONSUMER");
    assertThat(MDC.get("messaging.system")).isEqualTo("kafka");
    assertThat(MDC.get("messaging.destination")).isEqualTo("test-topic");

    interceptor.success(record, consumer);

    assertThat(TestAppender.events).isNotEmpty();
    ch.qos.logback.classic.spi.ILoggingEvent event = TestAppender.events.get(0);
    assertThat(event.getMDCPropertyMap().get("messaging.duration_ms")).isNotNull();
    assertThat(Integer.parseInt(event.getMDCPropertyMap().get("messaging.duration_ms")))
        .isGreaterThanOrEqualTo(0);
    assertThat(event.getLevel().toString()).isEqualTo("INFO");

    interceptor.afterRecord(record, consumer);
    assertThat(MDC.get("log_type")).isNull();
    assertThat(MDC.get("span.kind")).isNull();
    assertThat(MDC.get("messaging.system")).isNull();
    assertThat(MDC.get("messaging.destination")).isNull();
    assertThat(MDC.get("messaging.duration_ms")).isNull();
    assertThat(MDC.get("error.stacktrace")).isNull();
  }

  @Test
  void shouldLogAndPopulateMdcOnFailure() {
    interceptor.intercept(record, consumer);

    RuntimeException exception = new RuntimeException("Consumer error");
    interceptor.failure(record, exception, consumer);

    assertThat(TestAppender.events).isNotEmpty();
    ch.qos.logback.classic.spi.ILoggingEvent event = TestAppender.events.get(0);
    assertThat(event.getMDCPropertyMap().get("messaging.duration_ms")).isNotNull();
    assertThat(event.getMDCPropertyMap().get("error.stacktrace")).contains("Consumer error");
    assertThat(event.getLevel().toString()).isEqualTo("ERROR");

    interceptor.afterRecord(record, consumer);
    assertThat(MDC.get("log_type")).isNull();
  }

  @Test
  void shouldCleanUpInAfterRecordEvenIfSuccessOrFailureNotCalled() {
    interceptor.intercept(record, consumer);

    interceptor.afterRecord(record, consumer);
    assertThat(MDC.get("log_type")).isNull();

    TestAppender.clear();
    interceptor.success(record, consumer);
    assertThat(TestAppender.events).isEmpty();
  }
}
