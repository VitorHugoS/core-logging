package com.corelogging.filter;

import com.corelogging.config.CoreLoggingProperties;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.listener.RecordInterceptor;

public class CoreLoggingKafkaConsumerInterceptor<K, V> implements RecordInterceptor<K, V> {

  private static final Logger log =
      LoggerFactory.getLogger(CoreLoggingKafkaConsumerInterceptor.class);
  private final ThreadLocal<com.corelogging.scope.ObservabilityScope> scopeThreadLocal =
      new ThreadLocal<>();
  private final CoreLoggingProperties properties;

  public CoreLoggingKafkaConsumerInterceptor(CoreLoggingProperties properties) {
    this.properties = properties;
  }

  @Override
  public ConsumerRecord<K, V> intercept(ConsumerRecord<K, V> record, Consumer<K, V> consumer) {
    var scope = com.corelogging.scope.ObservabilityScope.start(log, "in_message", "CONSUMER");
    scope.tag("messaging.system", "kafka");
    scope.tag("messaging.destination", record.topic());

    String correlationId = null;
    for (String headerName : properties.getAcceptedCorrelationIdHeaders()) {
      Header correlationHeader = record.headers().lastHeader(headerName);
      if (correlationHeader != null && correlationHeader.value() != null) {
        String val = new String(correlationHeader.value(), StandardCharsets.UTF_8);
        if (!val.trim().isEmpty()) {
          correlationId = val;
          break;
        }
      }
    }

    if (correlationId == null) {
      correlationId = UUID.randomUUID().toString();
    }

    scope.tag("correlation_id", correlationId);

    scopeThreadLocal.set(scope);
    return record;
  }

  @Override
  public void success(ConsumerRecord<K, V> record, Consumer<K, V> consumer) {
    var scope = scopeThreadLocal.get();
    if (scope != null) {
      scope.computeDurationAs("messaging.duration_ms");
      scope.closeWith("Processed incoming kafka message from topic {}", record.topic());
    }
  }

  @Override
  public void failure(ConsumerRecord<K, V> record, Exception exception, Consumer<K, V> consumer) {
    var scope = scopeThreadLocal.get();
    if (scope != null) {
      scope.computeDurationAs("messaging.duration_ms");
      scope.recordError(exception);
      scope.closeWith(
          "Failed processing incoming kafka message from topic {}: {}",
          record.topic(),
          exception.getMessage());
    }
  }

  @Override
  public void afterRecord(ConsumerRecord<K, V> record, Consumer<K, V> consumer) {
    var scope = scopeThreadLocal.get();
    if (scope != null) {
      scope.close();
    }
    scopeThreadLocal.remove();
  }
}
