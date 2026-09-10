#!/bin/bash
cat << 'INNER_EOF' > src/test/java/com/corelogging/utils/LogSanitizerTest.java
package com.corelogging.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class LogSanitizerTest {

  @Test
  void shouldSanitizeSimpleMap() {
    Map<String, Object> map = new HashMap<>();
    map.put("name", "John");
    map.put("password", "secret");
    
    LogSanitizer.sanitizeMap(map, List.of("password"));
    
    assertThat(map.get("name")).isEqualTo("John");
    assertThat(map.get("password")).isEqualTo("***");
  }

  @Test
  void shouldSanitizeNestedMap() {
    Map<String, Object> map = new HashMap<>();
    Map<String, Object> nested = new HashMap<>();
    nested.put("token", "12345");
    nested.put("age", 30);
    map.put("user", nested);
    
    LogSanitizer.sanitizeMap(map, List.of("token"));
    
    @SuppressWarnings("unchecked")
    Map<String, Object> sanitizedNested = (Map<String, Object>) map.get("user");
    assertThat(sanitizedNested.get("age")).isEqualTo(30);
    assertThat(sanitizedNested.get("token")).isEqualTo("***");
  }

  @Test
  void shouldSanitizeListOfMaps() {
    Map<String, Object> map = new HashMap<>();
    List<Object> list = new ArrayList<>();
    Map<String, Object> item1 = new HashMap<>();
    item1.put("cpf", "111");
    list.add(item1);
    list.add("just-a-string");
    map.put("documents", list);
    
    LogSanitizer.sanitizeMap(map, List.of("cpf"));
    
    @SuppressWarnings("unchecked")
    List<Object> sanitizedList = (List<Object>) map.get("documents");
    @SuppressWarnings("unchecked")
    Map<String, Object> sanitizedItem = (Map<String, Object>) sanitizedList.get(0);
    assertThat(sanitizedItem.get("cpf")).isEqualTo("***");
    assertThat(sanitizedList.get(1)).isEqualTo("just-a-string");
  }

  @Test
  void shouldDoNothingIfMapOrFieldsAreNullOrEmpty() {
    Map<String, Object> map = new HashMap<>();
    map.put("password", "secret");
    LogSanitizer.sanitizeMap(map, null);
    assertThat(map.get("password")).isEqualTo("secret");

    LogSanitizer.sanitizeMap(map, List.of());
    assertThat(map.get("password")).isEqualTo("secret");

    LogSanitizer.sanitizeMap(null, List.of("password"));
  }
}
INNER_EOF
