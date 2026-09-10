#!/bin/bash
# Fix LogSanitizer (merge String and else, remove isEmpty)
cat << 'INNER_EOF' > src/main/java/com/corelogging/utils/LogSanitizer.java
package com.corelogging.utils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class LogSanitizer {

  public static void sanitizeMap(Map<String, Object> map, List<String> obfuscateFields) {
    if (map == null || obfuscateFields == null) {
      return;
    }

    for (Map.Entry<String, Object> entry : map.entrySet()) {
      String key = entry.getKey();
      Object value = entry.getValue();

      if (value instanceof Map) {
        @SuppressWarnings("unchecked")
        Map<String, Object> nestedMap = (Map<String, Object>) value;
        sanitizeMap(nestedMap, obfuscateFields);
      } else if (value instanceof List) {
        @SuppressWarnings("unchecked")
        List<Object> list = (List<Object>) value;
        list =
            list.stream()
                .map(
                    item -> {
                      if (item instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> nestedMap = (Map<String, Object>) item;
                        sanitizeMap(nestedMap, obfuscateFields);
                      }
                      return item;
                    })
                .collect(Collectors.toList());
        entry.setValue(list);
      } else {
        if (obfuscateFields.contains(key.toLowerCase())) {
          entry.setValue("***");
        }
      }
    }
  }
}
INNER_EOF

# Add test for CoreLogger.with(Object)
cat << 'INNER_EOF' >> src/test/java/com/corelogging/CoreLoggerTest.java

  @Test
  void shouldSanitizeObjectPayloadInWithMethod() {
    CoreLogger logger = new CoreLogger(log, new tools.jackson.databind.ObjectMapper(), java.util.List.of("password"));
    
    java.util.Map<String, Object> payload = new java.util.HashMap<>();
    payload.put("password", "secret");
    payload.put("user", "john");
    
    logger.info("msg").with(payload).log();

    assertThat(TestAppender.events).isNotEmpty();
    ch.qos.logback.classic.spi.ILoggingEvent event = TestAppender.events.get(0);
    assertThat(event.getMDCPropertyMap().get("password")).isEqualTo("***");
    assertThat(event.getMDCPropertyMap().get("user")).isEqualTo("john");
  }
INNER_EOF
sed -i '' -e '/shouldSanitizeObjectPayloadInWithMethod/,$d' src/test/java/com/corelogging/CoreLoggerTest.java
cat patch_pitest.sh | awk '/shouldSanitizeObjectPayloadInWithMethod/{p=1} p && /INNER_EOF/{p=0} p' | grep -v 'INNER_EOF' >> src/test/java/com/corelogging/CoreLoggerTest.java
echo "}" >> src/test/java/com/corelogging/CoreLoggerTest.java

# Add test for CoreLoggerFactory(properties != null)
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
INNER_EOF
sed -i '' -e '/shouldPassObfuscateFieldsToLogger/,$d' src/test/java/com/corelogging/CoreLoggerFactoryTest.java
cat patch_pitest.sh | awk '/shouldPassObfuscateFieldsToLogger/{p=1} p && /INNER_EOF/{p=0} p' | grep -v 'INNER_EOF' >> src/test/java/com/corelogging/CoreLoggerFactoryTest.java
echo "}" >> src/test/java/com/corelogging/CoreLoggerFactoryTest.java

