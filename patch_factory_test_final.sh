#!/bin/bash
sed -i '' -e '$d' src/test/java/com/corelogging/CoreLoggerFactoryTest.java
cat << 'INNER_EOF' >> src/test/java/com/corelogging/CoreLoggerFactoryTest.java

  @Test
  void shouldPassObfuscateFieldsToLogger() {
    com.corelogging.config.CoreLoggingProperties properties = new com.corelogging.config.CoreLoggingProperties();
    properties.getPayload().setObfuscateFields(java.util.List.of("token"));
    
    CoreLoggerFactory factory = new CoreLoggerFactory(new tools.jackson.databind.ObjectMapper(), properties);
    CoreLogger logger = factory.getLogger(CoreLoggerFactoryTest.class);
    
    logger.info("msg").with("token", "123").log();
    
    assertThat(com.corelogging.filter.TestAppender.events).isNotEmpty();
    assertThat(com.corelogging.filter.TestAppender.events.get(0).getMDCPropertyMap().get("token")).isEqualTo("***");
  }
}
INNER_EOF
