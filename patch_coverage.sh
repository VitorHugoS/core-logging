#!/bin/bash
cat << 'INNER_EOF' >> src/test/java/com/corelogging/utils/LogSanitizerTest.java

  @Test
  void shouldSanitizeNonStringValues() {
    Map<String, Object> map = new java.util.HashMap<>();
    map.put("password", 12345);
    LogSanitizer.sanitizeMap(map, java.util.List.of("password"));
    assertThat(map.get("password")).isEqualTo("***");
  }

  @Test
  void shouldCoverConstructor() {
    new LogSanitizer();
  }
}
INNER_EOF
sed -i '' -e '$d' src/test/java/com/corelogging/utils/LogSanitizerTest.java
cat patch_coverage.sh | grep -v '#!/bin/bash' | grep -v 'cat <<' | grep -v 'INNER_EOF' | grep -v 'sed' | grep -v 'cat patch_coverage.sh' >> src/test/java/com/corelogging/utils/LogSanitizerTest.java

cat << 'INNER_EOF2' >> src/test/java/com/corelogging/config/CoreLoggingAutoConfigurationTest.java

  @Test
  void shouldRegisterSecurityBeans() {
    CoreLoggingAutoConfiguration.SecurityConfiguration secConfig = new CoreLoggingAutoConfiguration.SecurityConfiguration();
    assertThat(secConfig.coreLoggingSecurityFilter()).isNotNull();
  }
}
INNER_EOF2
sed -i '' -e '$d' src/test/java/com/corelogging/config/CoreLoggingAutoConfigurationTest.java
cat patch_coverage.sh | grep -v '#!/bin/bash' | grep -v 'cat <<' | grep -v 'INNER_EOF' | grep -v 'sed' | grep -v 'cat patch_coverage.sh' >> src/test/java/com/corelogging/config/CoreLoggingAutoConfigurationTest.java
