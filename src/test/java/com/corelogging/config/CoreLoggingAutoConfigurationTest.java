package com.corelogging.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.corelogging.filter.CoreLoggingClientInterceptor;
import com.corelogging.filter.CoreLoggingFilter;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;

class CoreLoggingAutoConfigurationTest {

  @AfterEach
  void tearDown() {
    MDC.clear();
  }

  @Test
  void shouldDecorateTaskWithMdc() {
    CoreLoggingAutoConfiguration autoConfiguration = new CoreLoggingAutoConfiguration();
    TaskDecorator decorator = autoConfiguration.mdcTaskDecorator();

    MDC.put("test_key", "test_value");
    AtomicBoolean executed = new AtomicBoolean(false);

    Runnable decorated =
        decorator.decorate(
            () -> {
              assertThat(MDC.get("test_key")).isEqualTo("test_value");
              executed.set(true);
            });

    MDC.clear();
    decorated.run();

    assertThat(executed.get()).isTrue();
    assertThat(MDC.get("test_key")).isNull();
  }

  @Test
  void shouldDecorateTaskWithEmptyMdc() {
    CoreLoggingAutoConfiguration autoConfiguration = new CoreLoggingAutoConfiguration();
    TaskDecorator decorator = autoConfiguration.mdcTaskDecorator();

    AtomicBoolean executed = new AtomicBoolean(false);

    Runnable decorated =
        decorator.decorate(
            () -> {
              assertThat(MDC.get("test_key")).isNull();
              executed.set(true);
            });

    MDC.put("test_key", "should_be_cleared");
    decorated.run();

    assertThat(executed.get()).isTrue();
    assertThat(MDC.get("test_key")).isNull();
  }

  @Test
  void shouldRegisterBeans() {
    CoreLoggingAutoConfiguration autoConfiguration = new CoreLoggingAutoConfiguration();
    CoreLoggingProperties properties = new CoreLoggingProperties();

    CoreLoggingFilter filter = autoConfiguration.coreLoggingFilter(properties);
    com.corelogging.CoreLoggerFactory factory =
        autoConfiguration.coreLoggerFactory(new tools.jackson.databind.ObjectMapper());
    CoreLoggingClientInterceptor interceptor =
        autoConfiguration.coreLoggingClientInterceptor(new CoreLoggingProperties());

    assertThat(new CoreLoggingAutoConfiguration.FeignConfiguration().coreLoggingFeignCapability())
        .isNotNull();
    assertThat(
            new CoreLoggingAutoConfiguration.FeignConfiguration()
                .coreLoggingFeignRequestInterceptor(properties))
        .isNotNull();

    assertThat(filter).isNotNull();
    assertThat(interceptor).isNotNull();
    assertThat(factory).isNotNull();
  }
}
