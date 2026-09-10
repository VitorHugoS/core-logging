package com.corelogging.utils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class LogSanitizer {

  public static void sanitizeMap(Map<String, Object> map, List<String> obfuscateFields) {
    if (map == null || obfuscateFields == null) {
      return;
    }

    for (Map.Entry<String, Object> entry : map.entrySet()) {
      String key = entry.getKey();
      Object value = entry.getValue();

      if (value instanceof Map) {
        @SuppressWarnings("unchecked")
        Map<String, Object> nestedMap = (Map<String, Object>) value;
        sanitizeMap(nestedMap, obfuscateFields);
      } else if (value instanceof List) {
        @SuppressWarnings("unchecked")
        List<Object> list = (List<Object>) value;
        list =
            list.stream()
                .map(
                    item -> {
                      if (item instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> nestedMap = (Map<String, Object>) item;
                        sanitizeMap(nestedMap, obfuscateFields);
                      }
                      return item;
                    })
                .collect(Collectors.toList());
        entry.setValue(list);
      } else {
        if (obfuscateFields.contains(key.toLowerCase())) {
          entry.setValue("***");
        }
      }
    }
  }
}
