package com.corelogging.scope;

import static org.assertj.core.api.Assertions.assertThat;

import com.corelogging.filter.TestAppender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

class ObservabilityScopeTest {

  private static final Logger log = LoggerFactory.getLogger(ObservabilityScopeTest.class);

  @BeforeEach
  void setUp() {
    MDC.clear();
    TestAppender.clear();
  }

  @Test
  void shouldBeIdempotentOnClose() {
    ObservabilityScope scope = ObservabilityScope.start(log, "test", "TEST");
    scope.tag("my.tag", "value");

    assertThat(MDC.get("log_type")).isEqualTo("test");
    assertThat(MDC.get("my.tag")).isEqualTo("value");

    scope.close();
    assertThat(MDC.get("log_type")).isNull();
    assertThat(MDC.get("my.tag")).isNull();

    // Second close should not throw exception
    scope.close();
  }

  @Test
  void shouldCloseWithNoArgsInfo() {
    ObservabilityScope scope = ObservabilityScope.start(log, "test", "TEST");
    scope.closeWith("info_no_args");
    assertThat(MDC.get("log_type")).isNull();
  }

  @Test
  void shouldCloseWithNoArgsError() {
    ObservabilityScope scope = ObservabilityScope.start(log, "test", "TEST");
    scope.recordError(new RuntimeException("err"));
    scope.closeWith("error_no_args");
    assertThat(MDC.get("log_type")).isNull();
  }

  @Test
  void shouldAllowChainingTag() {
    ObservabilityScope scope = ObservabilityScope.start(log, "test", "TEST");
    ObservabilityScope returned = scope.tag("tag1", "v1").tag("tag2", "v2");
    assertThat(returned).isSameAs(scope);
    assertThat(MDC.get("tag1")).isEqualTo("v1");
    assertThat(MDC.get("tag2")).isEqualTo("v2");
  }

  @Test
  void shouldAllowChainingRecordError() {
    ObservabilityScope scope = ObservabilityScope.start(log, "test", "TEST");
    ObservabilityScope returned = scope.recordError(new RuntimeException());
    assertThat(returned).isSameAs(scope);
  }

  @Test
  void testHasError() {
    try (var scope = ObservabilityScope.start(log, "test", "TEST")) {
      assertThat(scope.hasError()).isFalse();
      scope.recordError(new RuntimeException());
      assertThat(scope.hasError()).isTrue();
    }
  }
}
