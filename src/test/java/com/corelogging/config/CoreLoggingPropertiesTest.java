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
    assertThat(properties.getPayload().getObfuscateFields())
        .containsExactly("password", "token", "cpf", "document");
  }

  @Test
  void shouldSetValues() {
    CoreLoggingProperties properties = new CoreLoggingProperties();
    CoreLoggingProperties.Payload payload = new CoreLoggingProperties.Payload();

    payload.setEnabled(true);
    payload.setObfuscateFields(List.of("secret"));
    properties.setPayload(payload);

    assertThat(properties.getPayload().isEnabled()).isTrue();
    assertThat(properties.getPayload().getObfuscateFields()).containsExactly("secret");
  }
}
