package com.corelogging.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.corelogging.filter.CoreLoggingKafkaConsumerInterceptor;
import org.junit.jupiter.api.Test;

class CoreLoggingKafkaAutoConfigurationTest {

  @Test
  void shouldRegisterBeans() {
    CoreLoggingKafkaAutoConfiguration autoConfiguration = new CoreLoggingKafkaAutoConfiguration();
    CoreLoggingKafkaConsumerInterceptor<Object, Object> interceptor =
        autoConfiguration.coreLoggingKafkaConsumerInterceptor();
    assertThat(interceptor).isNotNull();

    org.springframework.beans.factory.config.BeanPostProcessor postProcessor =
        autoConfiguration.coreLoggingKafkaContainerPostProcessor(interceptor);
    assertThat(postProcessor).isNotNull();

    @SuppressWarnings("unchecked")
    org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory<Object, Object>
        factory =
            org.mockito.Mockito.mock(
                org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory.class);

    postProcessor.postProcessAfterInitialization(factory, "testFactory");
    org.mockito.Mockito.verify(factory).setRecordInterceptor(interceptor);

    Object otherBean = new Object();
    assertThat(postProcessor.postProcessAfterInitialization(otherBean, "otherBean"))
        .isEqualTo(otherBean);
  }
}
