package com.corelogging;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class CoreLoggerFactoryTest {

  @Test
  void shouldCreateLogger() {
    CoreLoggerFactory factory = new CoreLoggerFactory(new ObjectMapper(), null);

    CoreLogger logger1 = factory.getLogger(CoreLoggerFactoryTest.class);
    assertThat(logger1).isNotNull();

    CoreLogger logger2 = factory.getLogger("custom-name");
    assertThat(logger2).isNotNull();
  }

  @Test
  void shouldPassObfuscateFieldsToLogger() {
    com.corelogging.filter.TestAppender.clear();
    com.corelogging.config.CoreLoggingProperties properties =
        new com.corelogging.config.CoreLoggingProperties();
    properties.getPayload().setObfuscateFields(java.util.List.of("token"));

    CoreLoggerFactory factory =
        new CoreLoggerFactory(new tools.jackson.databind.ObjectMapper(), properties);
    CoreLogger logger = factory.getLogger(CoreLoggerFactoryTest.class);

    logger.info("msg").with("token", "123").log();

    assertThat(com.corelogging.filter.TestAppender.events).isNotEmpty();
    assertThat(com.corelogging.filter.TestAppender.events.get(0).getMDCPropertyMap().get("token"))
        .isEqualTo("***");
  }

  @Test
  void shouldNotFailWhenPropertiesIsNull() {
    com.corelogging.filter.TestAppender.clear();
    CoreLoggerFactory factory =
        new CoreLoggerFactory(new tools.jackson.databind.ObjectMapper(), null);
    CoreLogger logger = factory.getLogger(CoreLoggerFactoryTest.class);

    logger.info("msg").with("token", "123").log();

    assertThat(com.corelogging.filter.TestAppender.events).isNotEmpty();
    assertThat(com.corelogging.filter.TestAppender.events.get(0).getMDCPropertyMap().get("token"))
        .isEqualTo("123");
  }
}
