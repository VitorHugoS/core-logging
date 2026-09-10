package com.corelogging;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class CoreLoggerFactoryTest {

  @Test
  void shouldCreateLogger() {
    CoreLoggerFactory factory = new CoreLoggerFactory(new ObjectMapper());

    CoreLogger logger1 = factory.getLogger(CoreLoggerFactoryTest.class);
    assertThat(logger1).isNotNull();

    CoreLogger logger2 = factory.getLogger("custom-name");
    assertThat(logger2).isNotNull();
  }
}
