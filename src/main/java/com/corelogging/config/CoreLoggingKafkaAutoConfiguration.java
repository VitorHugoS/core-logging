package com.corelogging.config;

import com.corelogging.filter.CoreLoggingKafkaConsumerInterceptor;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.KafkaTemplate;

@AutoConfiguration
@ConditionalOnClass(KafkaTemplate.class)
public class CoreLoggingKafkaAutoConfiguration {

  @Bean
  public CoreLoggingKafkaConsumerInterceptor<Object, Object> coreLoggingKafkaConsumerInterceptor(
      CoreLoggingProperties properties) {
    return new CoreLoggingKafkaConsumerInterceptor<>(properties);
  }

  @Bean
  public BeanPostProcessor coreLoggingKafkaContainerPostProcessor(
      CoreLoggingKafkaConsumerInterceptor<Object, Object> interceptor) {
    return new BeanPostProcessor() {
      @Override
      public Object postProcessAfterInitialization(Object bean, String beanName)
          throws BeansException {
        if (bean instanceof ConcurrentKafkaListenerContainerFactory) {
          @SuppressWarnings("unchecked")
          ConcurrentKafkaListenerContainerFactory<Object, Object> factory =
              (ConcurrentKafkaListenerContainerFactory<Object, Object>) bean;
          factory.setRecordInterceptor(interceptor);
        }
        return bean;
      }
    };
  }
}
