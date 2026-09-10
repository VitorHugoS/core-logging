package com.corelogging.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class CoreLoggingPropertiesTest {

  @Test
  void shouldHaveDefaultValues() {
    CoreLoggingProperties properties = new CoreLoggingProperties();
    assertThat(properties.getPayload()).isNotNull();
    assertThat(properties.getPayload().isEnabled()).isFalse();
    assertThat(properties.getPayload().getMaxCacheSize()).isEqualTo(1048576);
    assertThat(properties.getPayload().getMaxLength()).isEqualTo(10000);
    assertThat(properties.getPayload().getObfuscateFields())
        .containsExactly("password", "token", "cpf", "document");
    assertThat(properties.getCorrelationIdHeader()).isEqualTo("x-correlation-id");
    assertThat(properties.getAcceptedCorrelationIdHeaders())
        .containsExactly("x-correlation-id", "x-request-id", "correlation-id", "traceparent", "b3");
  }

  @Test
  void shouldSetValues() {
    CoreLoggingProperties properties = new CoreLoggingProperties();
    CoreLoggingProperties.Payload payload = new CoreLoggingProperties.Payload();

    payload.setEnabled(true);
    payload.setMaxCacheSize(2048);
    payload.setMaxLength(500);
    payload.setObfuscateFields(List.of("secret"));
    properties.setCorrelationIdHeader("x-custom-id");
    properties.setAcceptedCorrelationIdHeaders(List.of("x-custom-id", "custom-trace"));

    properties.setPayload(payload);

    assertThat(properties.getPayload().isEnabled()).isTrue();
    assertThat(properties.getPayload().getMaxCacheSize()).isEqualTo(2048);
    assertThat(properties.getPayload().getMaxLength()).isEqualTo(500);
    assertThat(properties.getPayload().getObfuscateFields()).containsExactly("secret");
    assertThat(properties.getCorrelationIdHeader()).isEqualTo("x-custom-id");
    assertThat(properties.getAcceptedCorrelationIdHeaders())
        .containsExactly("x-custom-id", "custom-trace");
  }
}
