package com.corelogging.config;

import com.corelogging.CoreLoggerFactory;
import com.corelogging.filter.CoreLoggingClientInterceptor;
import com.corelogging.filter.CoreLoggingFeignCapability;
import com.corelogging.filter.CoreLoggingFilter;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.TaskDecorator;

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
  @ConditionalOnMissingBean
  public TaskDecorator mdcTaskDecorator() {
    return runnable -> {
      var contextMap = MDC.getCopyOfContextMap();
      return () -> {
        try {
          if (contextMap != null) {
            MDC.setContextMap(contextMap);
          } else {
            MDC.clear();
          }
          runnable.run();
        } finally {
          MDC.clear();
        }
      };
    };
  }

  @Bean
  public CoreLoggingClientInterceptor coreLoggingClientInterceptor(
      CoreLoggingProperties properties) {
    return new CoreLoggingClientInterceptor(properties);
  }

  @ConditionalOnClass(name = "feign.Capability")
  static class FeignConfiguration {
    @Bean
    public CoreLoggingFeignCapability coreLoggingFeignCapability() {
      return new CoreLoggingFeignCapability();
    }

    @Bean
    @ConditionalOnMissingBean
    public com.corelogging.filter.CoreLoggingFeignRequestInterceptor
        coreLoggingFeignRequestInterceptor(CoreLoggingProperties properties) {
      return new com.corelogging.filter.CoreLoggingFeignRequestInterceptor(properties);
    }
  }
}
