package com.corelogging.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.corelogging.filter.CoreLoggingClientInterceptor;
import com.corelogging.filter.CoreLoggingFilter;
import org.junit.jupiter.api.Test;

class CoreLoggingAutoConfigurationTest {

  @Test
  void shouldRegisterBeans() {
    CoreLoggingAutoConfiguration autoConfiguration = new CoreLoggingAutoConfiguration();
    CoreLoggingProperties properties = new CoreLoggingProperties();

    CoreLoggingFilter filter = autoConfiguration.coreLoggingFilter(properties);
    com.corelogging.CoreLoggerFactory factory =
        autoConfiguration.coreLoggerFactory(new tools.jackson.databind.ObjectMapper());
    CoreLoggingClientInterceptor interceptor = autoConfiguration.coreLoggingClientInterceptor();
    assertThat(new CoreLoggingAutoConfiguration.FeignConfiguration().coreLoggingFeignCapability())
        .isNotNull();

    assertThat(filter).isNotNull();
    assertThat(interceptor).isNotNull();
    assertThat(factory).isNotNull();
  }
}
