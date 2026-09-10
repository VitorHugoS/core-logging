package com.corelogging.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.corelogging.filter.CoreLoggingKafkaConsumerInterceptor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;

@ExtendWith(MockitoExtension.class)
class CoreLoggingKafkaAutoConfigurationTest {

  @Mock private ConcurrentKafkaListenerContainerFactory<Object, Object> factory;

  @Test
  void shouldRegisterBeans() {
    CoreLoggingKafkaAutoConfiguration autoConfiguration = new CoreLoggingKafkaAutoConfiguration();
    CoreLoggingKafkaConsumerInterceptor<Object, Object> interceptor =
        autoConfiguration.coreLoggingKafkaConsumerInterceptor(new CoreLoggingProperties());
    assertThat(interceptor).isNotNull();

    org.springframework.beans.factory.config.BeanPostProcessor postProcessor =
        autoConfiguration.coreLoggingKafkaContainerPostProcessor(interceptor);
    assertThat(postProcessor).isNotNull();

    postProcessor.postProcessAfterInitialization(factory, "testFactory");
    org.mockito.Mockito.verify(factory).setRecordInterceptor(interceptor);

    Object otherBean = new Object();
    assertThat(postProcessor.postProcessAfterInitialization(otherBean, "otherBean"))
        .isEqualTo(otherBean);
  }
}
