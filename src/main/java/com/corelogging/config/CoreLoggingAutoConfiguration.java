package com.corelogging.config;

import com.corelogging.filter.CoreLoggingClientInterceptor;
import com.corelogging.filter.CoreLoggingFeignCapability;
import com.corelogging.filter.CoreLoggingFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@EnableConfigurationProperties(CoreLoggingProperties.class)
@ConditionalOnWebApplication
public class CoreLoggingAutoConfiguration {

  @Bean
  public CoreLoggingFilter coreLoggingFilter(CoreLoggingProperties properties) {
    return new CoreLoggingFilter(properties);
  }

  @Bean
  public CoreLoggingClientInterceptor coreLoggingClientInterceptor() {
    return new CoreLoggingClientInterceptor();
  }

  @Bean
  @ConditionalOnClass(name = "feign.Capability")
  public CoreLoggingFeignCapability coreLoggingFeignCapability() {
    return new CoreLoggingFeignCapability();
  }
}
