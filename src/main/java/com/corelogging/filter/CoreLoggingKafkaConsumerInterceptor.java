package com.corelogging.filter;

import java.io.PrintWriter;
import java.io.StringWriter;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.listener.RecordInterceptor;

public class CoreLoggingKafkaConsumerInterceptor<K, V> implements RecordInterceptor<K, V> {

  private static final Logger log =
      LoggerFactory.getLogger(CoreLoggingKafkaConsumerInterceptor.class);
  private final ThreadLocal<Long> startTime = new ThreadLocal<>();

  @Override
  public ConsumerRecord<K, V> intercept(ConsumerRecord<K, V> record, Consumer<K, V> consumer) {
    startTime.set(System.currentTimeMillis());
    MDC.put("log_type", "in_message");
    MDC.put("span.kind", "CONSUMER");
    MDC.put("messaging.system", "kafka");
    MDC.put("messaging.destination", record.topic());
    return record;
  }

  @Override
  public void success(ConsumerRecord<K, V> record, Consumer<K, V> consumer) {
    Long start = startTime.get();
    if (start != null) {
      MDC.put("messaging.duration_ms", String.valueOf(System.currentTimeMillis() - start));
    }
    log.info("Processed incoming kafka message from topic {}", record.topic());
  }

  @Override
  public void failure(ConsumerRecord<K, V> record, Exception exception, Consumer<K, V> consumer) {
    Long start = startTime.get();
    if (start != null) {
      MDC.put("messaging.duration_ms", String.valueOf(System.currentTimeMillis() - start));
    }

    StringWriter sw = new StringWriter();
    exception.printStackTrace(new PrintWriter(sw));
    MDC.put("error.stacktrace", sw.toString());

    log.error(
        "Failed processing incoming kafka message from topic {}: {}",
        record.topic(),
        exception.getMessage(),
        exception);
  }

  @Override
  public void afterRecord(ConsumerRecord<K, V> record, Consumer<K, V> consumer) {
    startTime.remove();
    MDC.remove("log_type");
    MDC.remove("span.kind");
    MDC.remove("messaging.system");
    MDC.remove("messaging.destination");
    MDC.remove("messaging.duration_ms");
    MDC.remove("error.stacktrace");
  }
}
