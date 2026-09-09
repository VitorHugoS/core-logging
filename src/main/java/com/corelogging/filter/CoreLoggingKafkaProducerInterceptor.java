package com.corelogging.filter;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;
import org.apache.kafka.clients.producer.ProducerInterceptor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

public class CoreLoggingKafkaProducerInterceptor<K, V> implements ProducerInterceptor<K, V> {

  private static final Logger log =
      LoggerFactory.getLogger(CoreLoggingKafkaProducerInterceptor.class);

  @Override
  public ProducerRecord<K, V> onSend(ProducerRecord<K, V> record) {

    String previousLogType = MDC.get("log_type");
    String previousSpanKind = MDC.get("span.kind");

    try {
      MDC.put("log_type", "out_message");
      MDC.put("span.kind", "PRODUCER");
      MDC.put("messaging.system", "kafka");
      MDC.put("messaging.destination", record.topic());

      log.info("Sending outgoing kafka message to topic {}", record.topic());
    } finally {

      if (previousLogType != null) {
        MDC.put("log_type", previousLogType);
      } else {
        MDC.remove("log_type");
      }

      if (previousSpanKind != null) {
        MDC.put("span.kind", previousSpanKind);
      } else {
        MDC.remove("span.kind");
      }

      MDC.remove("messaging.system");
      MDC.remove("messaging.destination");
    }

    return record;
  }

  @Override
  public void onAcknowledgement(RecordMetadata metadata, Exception exception) {

    if (exception != null) {
      String topic = metadata != null ? metadata.topic() : "unknown";
      MDC.put("log_type", "out_message");
      MDC.put("span.kind", "PRODUCER");
      MDC.put("messaging.system", "kafka");
      MDC.put("messaging.destination", topic);

      StringWriter sw = new StringWriter();
      exception.printStackTrace(new PrintWriter(sw));
      MDC.put("error.stacktrace", sw.toString());

      try {
        log.error(
            "Failed to send kafka message to topic {}: {}",
            topic,
            exception.getMessage(),
            exception);
      } finally {
        MDC.remove("log_type");
        MDC.remove("span.kind");
        MDC.remove("messaging.system");
        MDC.remove("messaging.destination");
        MDC.remove("error.stacktrace");
      }
    }
  }

  @Override
  public void close() {}

  @Override
  public void configure(Map<String, ?> configs) {}
}
