package com.corelogging.filter;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class PayloadObfuscatorTest {

  @Test
  void shouldObfuscateJsonStringFields() {
    PayloadObfuscator obfuscator = new PayloadObfuscator(List.of("password", "cpf"), 100);
    String json =
        "{\"user\": \"vitor\", \"password\": \"secreta123\", \"age\": 30, \"cpf\": \"123.456.789-00\"}";

    String result = obfuscator.process(json);

    assertThat(result).contains("\"password\": \"***\"");
    assertThat(result).contains("\"cpf\": \"***\"");
    assertThat(result).contains("\"user\": \"vitor\"");
    assertThat(result).contains("\"age\": 30");
    assertThat(result).doesNotContain("secreta123");
    assertThat(result).doesNotContain("123.456.789-00");
  }

  @Test
  void shouldObfuscateJsonNumberFields() {
    PayloadObfuscator obfuscator = new PayloadObfuscator(List.of("token"), 100);
    String json = "{\"token\": 123456789, \"valid\": true}";

    String result = obfuscator.process(json);

    assertThat(result).contains("\"token\": \"***\"");
    assertThat(result).contains("\"valid\": true");
    assertThat(result).doesNotContain("123456789");
  }

  @Test
  void shouldTruncateLongPayloads() {
    PayloadObfuscator obfuscator = new PayloadObfuscator(List.of("password"), 10);
    String json = "{\"password\": \"secret\", \"other\": \"very long string that will be cut\"}";

    String result = obfuscator.process(json);

    assertThat(result).hasSize(10);
    assertThat(result).isEqualTo("{\"password"); // 10 chars
  }

  @Test
  void shouldReturnSameIfEmptyOrNull() {
    PayloadObfuscator obfuscator = new PayloadObfuscator(List.of("password"), 10);
    assertThat(obfuscator.process("")).isEqualTo("");
    assertThat(obfuscator.process(null)).isNull();
  }

  @Test
  void shouldReturnSameIfNoFieldsConfigured() {
    PayloadObfuscator obfuscator = new PayloadObfuscator(List.of(), 100);
    String json = "{\"\": \"secret\"}";
    assertThat(obfuscator.process(json)).isEqualTo(json);

    PayloadObfuscator obfuscator2 = new PayloadObfuscator(null, 100);
    assertThat(obfuscator2.process(json)).isEqualTo(json);
  }
}
