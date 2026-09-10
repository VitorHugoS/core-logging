package com.corelogging.trace;

import static org.assertj.core.api.Assertions.assertThat;

import com.corelogging.filter.TestAppender;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

class DefaultTraceManagerTest {

  private DefaultTraceManager traceManager;

  @BeforeEach
  void setUp() {
    traceManager = new DefaultTraceManager();
    MDC.clear();
    TestAppender.clear();
  }

  @AfterEach
  void tearDown() {
    MDC.clear();
    TestAppender.clear();
  }

  @Test
  void testObserveSuccess() throws Exception {
    traceManager.observe(
        "my_log_type",
        "SERVER",
        "success action",
        context -> {
          context.tag("my_tag", "tag_value");
          assertThat(context.hasError()).isFalse();
        });

    assertThat(MDC.get("log_type")).isNull();
    assertThat(TestAppender.events).hasSize(1);
    ch.qos.logback.classic.spi.ILoggingEvent event = TestAppender.events.get(0);
    Map<String, String> mdcMap = event.getMDCPropertyMap();

    assertThat(event.getFormattedMessage()).isEqualTo("Processed success action");
    assertThat(mdcMap).containsEntry("log_type", "my_log_type");
    assertThat(mdcMap).containsEntry("span.kind", "SERVER");
    assertThat(mdcMap).containsEntry("my_tag", "tag_value");
    assertThat(mdcMap.containsKey("http.duration_ms")).isTrue();

    // Verify context.cleanup() ran
    assertThat(MDC.get("my_tag")).isNull();
    assertThat(MDC.get("http.duration_ms")).isNull();
  }

  @Test
  void testObserveWithPreviousMdc() throws Exception {
    MDC.put("log_type", "old_type");
    MDC.put("span.kind", "old_kind");

    traceManager.observe(
        "my_log_type",
        "SERVER",
        "success action",
        context -> {
          assertThat(MDC.get("log_type")).isEqualTo("my_log_type");
        });

    assertThat(MDC.get("log_type")).isEqualTo("old_type");
    assertThat(MDC.get("span.kind")).isEqualTo("old_kind");
  }

  @Test
  void testObserveException() {
    try {
      traceManager.observe(
          "my_log_type",
          "SERVER",
          "failed action",
          context -> {
            throw new Exception("checked exception");
          });
    } catch (Exception e) {
      assertThat(e).isInstanceOf(Exception.class).hasMessage("checked exception");
    }

    assertThat(TestAppender.events).hasSize(1);
    ch.qos.logback.classic.spi.ILoggingEvent event = TestAppender.events.get(0);
    assertThat(event.getFormattedMessage()).isEqualTo("Failed failed action: checked exception");
    assertThat(event.getMDCPropertyMap().containsKey("error.stacktrace")).isTrue();
  }

  @Test
  void testObserveRuntimeException() {
    try {
      traceManager.observe(
          "my_log_type",
          "SERVER",
          "failed action",
          context -> {
            throw new RuntimeException("runtime exception");
          });
    } catch (Exception e) {
      assertThat(e).isInstanceOf(RuntimeException.class).hasMessage("runtime exception");
    }

    assertThat(TestAppender.events).hasSize(1);
  }

  @Test
  void testObserveThrowable() {
    try {
      traceManager.observe(
          "my_log_type",
          "SERVER",
          "failed action",
          context -> {
            throw new OutOfMemoryError("out of memory");
          });
    } catch (Exception e) {
      assertThat(e).isInstanceOf(RuntimeException.class);
      assertThat(e.getCause()).isInstanceOf(OutOfMemoryError.class);
    }

    assertThat(TestAppender.events).hasSize(1);
    ch.qos.logback.classic.spi.ILoggingEvent event = TestAppender.events.get(0);
    assertThat(event.getFormattedMessage()).startsWith("Failed failed action");
    assertThat(event.getMDCPropertyMap().containsKey("error.stacktrace")).isTrue();
  }

  @Test
  void testTraceContextTagNullValues() throws Exception {
    traceManager.observe(
        "my_log_type",
        "SERVER",
        "success action",
        context -> {
          context.tag(null, "value");
          context.tag("key", null);
          context.recordError(null);
        });

    assertThat(TestAppender.events).hasSize(1);
    Map<String, String> mdcMap = TestAppender.events.get(0).getMDCPropertyMap();
    assertThat(mdcMap.containsKey("null")).isFalse();
    assertThat(mdcMap.containsKey("key")).isFalse();
    assertThat(mdcMap.containsKey("error.stacktrace")).isFalse();
  }
}
