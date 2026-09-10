package com.corelogging.config;

import com.corelogging.CoreLoggerFactory;
import com.corelogging.filter.CoreLoggingClientInterceptor;
import com.corelogging.filter.CoreLoggingFeignCapability;
import com.corelogging.filter.CoreLoggingFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@EnableConfigurationProperties(CoreLoggingProperties.class)
@ConditionalOnWebApplication
public class CoreLoggingAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean
  public CoreLoggerFactory coreLoggerFactory(tools.jackson.databind.ObjectMapper objectMapper) {
    return new CoreLoggerFactory(objectMapper);
  }

  @Bean
  public CoreLoggingFilter coreLoggingFilter(CoreLoggingProperties properties) {
    return new CoreLoggingFilter(properties);
  }

  @Bean
  public CoreLoggingClientInterceptor coreLoggingClientInterceptor() {
    return new CoreLoggingClientInterceptor();
  }

  @ConditionalOnClass(name = "feign.Capability")
  static class FeignConfiguration {
    @Bean
    public CoreLoggingFeignCapability coreLoggingFeignCapability() {
      return new CoreLoggingFeignCapability();
    }
  }
}
