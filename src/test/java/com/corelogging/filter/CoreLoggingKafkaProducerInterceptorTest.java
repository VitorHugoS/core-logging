package com.corelogging.filter;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

class CoreLoggingKafkaProducerInterceptorTest {

  private CoreLoggingKafkaProducerInterceptor<Object, Object> interceptor;
  private ProducerRecord<Object, Object> record;

  @BeforeEach
  void setUp() {
    interceptor = new CoreLoggingKafkaProducerInterceptor<>();
    record = new ProducerRecord<>("out-topic", "value");
    MDC.clear();
    TestAppender.clear();
  }

  @Test
  void shouldLogAndPopulateMdcOnSend() {
    ProducerRecord<Object, Object> returned = interceptor.onSend(record);
    assertThat(returned).isEqualTo(record);

    assertThat(MDC.get("log_type")).isNull();

    assertThat(TestAppender.events).isNotEmpty();
    ch.qos.logback.classic.spi.ILoggingEvent event = TestAppender.events.get(0);
    assertThat(event.getMDCPropertyMap().get("log_type")).isEqualTo("out_message");
    assertThat(event.getMDCPropertyMap().get("span.kind")).isEqualTo("PRODUCER");
    assertThat(event.getMDCPropertyMap().get("messaging.system")).isEqualTo("kafka");
    assertThat(event.getMDCPropertyMap().get("messaging.destination")).isEqualTo("out-topic");
    assertThat(event.getLevel().toString()).isEqualTo("INFO");
  }

  @Test
  void shouldRestorePreviousMdcStateOnSend() {
    MDC.put("log_type", "previous_type");
    MDC.put("span.kind", "previous_span");

    interceptor.onSend(record);

    assertThat(MDC.get("log_type")).isEqualTo("previous_type");
    assertThat(MDC.get("span.kind")).isEqualTo("previous_span");
  }

  @Test
  void shouldLogAndPopulateMdcOnAcknowledgementFailure() {
    RecordMetadata metadata = new RecordMetadata(new TopicPartition("out-topic", 0), 0, 0, 0, 0, 0);
    RuntimeException exception = new RuntimeException("Kafka timeout");

    interceptor.onAcknowledgement(metadata, exception);

    assertThat(MDC.get("log_type")).isNull();

    assertThat(TestAppender.events).isNotEmpty();
    ch.qos.logback.classic.spi.ILoggingEvent event = TestAppender.events.get(0);
    assertThat(event.getMDCPropertyMap().get("log_type")).isEqualTo("out_message");
    assertThat(event.getMDCPropertyMap().get("messaging.destination")).isEqualTo("out-topic");
    assertThat(event.getMDCPropertyMap().get("error.stacktrace")).contains("Kafka timeout");
    assertThat(event.getLevel().toString()).isEqualTo("ERROR");
  }

  @Test
  void shouldLogAndPopulateMdcOnAcknowledgementFailureWithNullMetadata() {
    RuntimeException exception = new RuntimeException("Kafka timeout");

    interceptor.onAcknowledgement(null, exception);

    assertThat(MDC.get("log_type")).isNull();

    assertThat(TestAppender.events).isNotEmpty();
    ch.qos.logback.classic.spi.ILoggingEvent event = TestAppender.events.get(0);
    assertThat(event.getMDCPropertyMap().get("messaging.destination")).isEqualTo("unknown");
    assertThat(event.getLevel().toString()).isEqualTo("ERROR");
  }

  @Test
  void shouldDoNothingOnAcknowledgementSuccess() {
    RecordMetadata metadata = new RecordMetadata(new TopicPartition("out-topic", 0), 0, 0, 0, 0, 0);
    interceptor.onAcknowledgement(metadata, null);

    assertThat(TestAppender.events).isEmpty();
  }

  @Test
  void shouldCoverEmptyMethods() {
    interceptor.close();
    interceptor.configure(Collections.emptyMap());
  }

  @Test
  void shouldAddCorrelationIdHeaderIfPresentInMdc() {
    MDC.put("correlation_id", "my-kafka-corr-id");
    ProducerRecord<Object, Object> returned = interceptor.onSend(record);
    assertThat(returned).isEqualTo(record);

    org.apache.kafka.common.header.Header header =
        returned.headers().lastHeader("x-correlation-id");
    assertThat(header).isNotNull();
    assertThat(new String(header.value(), java.nio.charset.StandardCharsets.UTF_8))
        .isEqualTo("my-kafka-corr-id");
  }

  @Test
  void shouldNotAddCorrelationIdHeaderIfNotPresentInMdc() {
    ProducerRecord<Object, Object> returned = interceptor.onSend(record);
    assertThat(returned.headers().lastHeader("x-correlation-id")).isNull();
  }
}
