#!/bin/bash
sed -i '' -e '$d' src/main/java/com/corelogging/config/CoreLoggingAutoConfiguration.java
cat << 'INNER_EOF' >> src/main/java/com/corelogging/config/CoreLoggingAutoConfiguration.java

  @ConditionalOnClass(name = "org.springframework.security.core.context.SecurityContextHolder")
  static class SecurityConfiguration {
    @Bean
    public com.corelogging.filter.CoreLoggingSecurityFilter coreLoggingSecurityFilter() {
      return new com.corelogging.filter.CoreLoggingSecurityFilter();
    }
  }
}
INNER_EOF
