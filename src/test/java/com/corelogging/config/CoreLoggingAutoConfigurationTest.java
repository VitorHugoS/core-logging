package com.corelogging.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.corelogging.filter.CoreLoggingClientInterceptor;
import com.corelogging.filter.CoreLoggingFeignCapability;
import com.corelogging.filter.CoreLoggingFilter;
import org.junit.jupiter.api.Test;

class CoreLoggingAutoConfigurationTest {

  @Test
  void shouldRegisterBeans() {
    CoreLoggingAutoConfiguration autoConfiguration = new CoreLoggingAutoConfiguration();
    CoreLoggingProperties properties = new CoreLoggingProperties();

    CoreLoggingFilter filter = autoConfiguration.coreLoggingFilter(properties);
    CoreLoggingClientInterceptor interceptor = autoConfiguration.coreLoggingClientInterceptor();
    CoreLoggingFeignCapability feignCapability = autoConfiguration.coreLoggingFeignCapability();

    assertThat(filter).isNotNull();
    assertThat(interceptor).isNotNull();
    assertThat(feignCapability).isNotNull();
  }
}
