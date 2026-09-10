#!/bin/bash
sed -i '' -e '$d' src/test/java/com/corelogging/config/CoreLoggingAutoConfigurationTest.java
cat << 'INNER_EOF' >> src/test/java/com/corelogging/config/CoreLoggingAutoConfigurationTest.java

  @Test
  void shouldRegisterSecurityBeans() {
    CoreLoggingAutoConfiguration.SecurityConfiguration secConfig = new CoreLoggingAutoConfiguration.SecurityConfiguration();
    assertThat(secConfig.coreLoggingSecurityFilter()).isNotNull();
  }
}
INNER_EOF
