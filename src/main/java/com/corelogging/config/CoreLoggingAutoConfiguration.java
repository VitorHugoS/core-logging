package com.corelogging.config;

import com.corelogging.filter.CoreLoggingFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@EnableConfigurationProperties(CoreLoggingProperties.class)
@ConditionalOnWebApplication // Somente carrega este filtro se a aplicação for Web (REST)
public class CoreLoggingAutoConfiguration {

  @Bean
  public CoreLoggingFilter coreLoggingFilter(CoreLoggingProperties properties) {
    return new CoreLoggingFilter(properties);
  }

  @Bean
  public com.corelogging.filter.CoreLoggingClientInterceptor coreLoggingClientInterceptor() {
    return new com.corelogging.filter.CoreLoggingClientInterceptor();
  }
}
