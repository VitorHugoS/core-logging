#!/bin/bash
sed -i '' -e '$d' src/test/java/com/corelogging/CoreLoggerTest.java
cat << 'INNER_EOF' >> src/test/java/com/corelogging/CoreLoggerTest.java

  @Test
  void shouldSanitizeStringValuesInWithMethod() {
    CoreLogger logger = new CoreLogger(slf4jLogger, objectMapper, java.util.List.of("password"));
    logger.info("msg").with("password", "secret123").with("user", "john").log();

    org.mockito.Mockito.verify(slf4jLogger).info("msg");
    assertThat(TestAppender.events).isNotEmpty();
    ch.qos.logback.classic.spi.ILoggingEvent event = TestAppender.events.get(0);
    assertThat(event.getMDCPropertyMap().get("password")).isEqualTo("***");
    assertThat(event.getMDCPropertyMap().get("user")).isEqualTo("john");
  }

  @Test
  void shouldSanitizeObjectValuesInWithMethod() {
    CoreLogger logger = new CoreLogger(slf4jLogger, objectMapper, java.util.List.of("token"));
    logger.info("msg").with("token", 12345).with("age", 30).log();

    org.mockito.Mockito.verify(slf4jLogger).info("msg");
    assertThat(TestAppender.events).isNotEmpty();
    ch.qos.logback.classic.spi.ILoggingEvent event = TestAppender.events.get(0);
    assertThat(event.getMDCPropertyMap().get("token")).isEqualTo("***");
    assertThat(event.getMDCPropertyMap().get("age")).isEqualTo("30");
  }
}
INNER_EOF
