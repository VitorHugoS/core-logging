package com.corelogging.propagation;

import static org.assertj.core.api.Assertions.assertThat;

import com.corelogging.config.CoreLoggingProperties;
import io.micrometer.tracing.propagation.Propagator.Getter;
import io.micrometer.tracing.propagation.Propagator.Setter;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

class CoreLoggingPropagatorTest {

  private CoreLoggingProperties properties;
  private CoreLoggingPropagator propagator;

  @BeforeEach
  void setUp() {
    properties = new CoreLoggingProperties();
    propagator = new CoreLoggingPropagator(properties);
    MDC.clear();
  }

  @AfterEach
  void tearDown() {
    MDC.clear();
  }

  @Test
  void testFields() {
    assertThat(propagator.fields())
        .containsExactlyElementsOf(properties.getAcceptedCorrelationIdHeaders());
  }

  @Test
  void testInject() {
    MDC.put("correlation_id", "my-id");
    Map<String, String> carrier = new HashMap<>();
    Setter<Map<String, String>> setter = Map::put;

    propagator.inject(null, carrier, setter);

    assertThat(carrier).containsEntry("x-correlation-id", "my-id");
  }

  @Test
  void testInjectNullMdc() {
    Map<String, String> carrier = new HashMap<>();
    Setter<Map<String, String>> setter = Map::put;

    propagator.inject(null, carrier, setter);

    assertThat(carrier).isEmpty();
  }

  @Test
  void testExtractAndExtractId() {
    Map<String, String> carrier = new HashMap<>();
    carrier.put("x-request-id", "req-123");
    Getter<Map<String, String>> getter = Map::get;

    String id = propagator.extractId(carrier, getter);
    assertThat(id).isEqualTo("req-123");

    // test dummy extract
    assertThat(propagator.extract(carrier, getter)).isNotNull();
  }

  @Test
  void testExtractFallbackToTraceId() {
    Map<String, String> carrier = new HashMap<>();
    Getter<Map<String, String>> getter = Map::get;
    MDC.put("traceId", "trace-456");

    String id = propagator.extractId(carrier, getter);
    assertThat(id).isEqualTo("trace-456");
  }

  @Test
  void testExtractFallbackToUUID() {
    Map<String, String> carrier = new HashMap<>();
    Getter<Map<String, String>> getter = Map::get;

    String id = propagator.extractId(carrier, getter);
    assertThat(id).isNotNull().isNotEmpty();
  }
}
