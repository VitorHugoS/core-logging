package com.corelogging.filter;

import java.util.Map;
import org.apache.kafka.clients.producer.ProducerInterceptor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CoreLoggingKafkaProducerInterceptor<K, V> implements ProducerInterceptor<K, V> {

  private static final Logger log =
      LoggerFactory.getLogger(CoreLoggingKafkaProducerInterceptor.class);

  @Override
  public ProducerRecord<K, V> onSend(ProducerRecord<K, V> record) {
    var scope = com.corelogging.scope.ObservabilityScope.start(log, "out_message", "PRODUCER");
    scope.tag("messaging.system", "kafka");
    scope.tag("messaging.destination", record.topic());

    // the kafka producer is completely asynchronous. onSend just adds to a queue.
    // so we can't wrap it in a try-finally properly here to capture duration because it doesn't
    // block.
    // However, onSend is called on the caller thread. It just formats the log.
    try {
      log.info("Sending outgoing kafka message to topic {}", record.topic());
    } finally {
      scope.close();
    }

    return record;
  }

  @Override
  public void onAcknowledgement(RecordMetadata metadata, Exception exception) {
    if (exception != null) {
      String topic = metadata != null ? metadata.topic() : "unknown";
      try (var scope =
          com.corelogging.scope.ObservabilityScope.start(log, "out_message", "PRODUCER")) {
        scope.tag("messaging.system", "kafka");
        scope.tag("messaging.destination", topic);
        scope.recordError(exception);
        scope.closeWith(
            "Failed to send kafka message to topic {}: {}", topic, exception.getMessage());
      }
    }
  }

  @Override
  public void close() {}

  @Override
  public void configure(Map<String, ?> configs) {}
}
