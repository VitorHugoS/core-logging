package com.corelogging.filter;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.listener.RecordInterceptor;

public class CoreLoggingKafkaConsumerInterceptor<K, V> implements RecordInterceptor<K, V> {

  private static final Logger log =
      LoggerFactory.getLogger(CoreLoggingKafkaConsumerInterceptor.class);
  private final ThreadLocal<com.corelogging.scope.ObservabilityScope> scopeThreadLocal =
      new ThreadLocal<>();

  @Override
  public ConsumerRecord<K, V> intercept(ConsumerRecord<K, V> record, Consumer<K, V> consumer) {
    var scope = com.corelogging.scope.ObservabilityScope.start(log, "in_message", "CONSUMER");
    scope.tag("messaging.system", "kafka");
    scope.tag("messaging.destination", record.topic());
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
