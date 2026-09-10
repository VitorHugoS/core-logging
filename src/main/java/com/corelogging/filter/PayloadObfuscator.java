package com.corelogging.filter;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PayloadObfuscator {

  private final Pattern pattern;
  private final int maxLength;

  public PayloadObfuscator(List<String> obfuscateFields, int maxLength) {
    this.maxLength = maxLength;
    if (obfuscateFields == null || obfuscateFields.isEmpty()) {
      this.pattern = null;
    } else {
      String fields = String.join("|", obfuscateFields);
      this.pattern =
          Pattern.compile(
              "(\"(" + fields + ")\"\\s*:\\s*)(?:\"([^\"]+)\"|([^,\\}\\s]+))",
              Pattern.CASE_INSENSITIVE);
    }
  }

  public String process(String payload) {
    if (payload == null) {
      return null;
    }

    String result = payload;

    if (pattern != null) {
      Matcher matcher = pattern.matcher(result);
      result = matcher.replaceAll("$1\"***\"");
    }

    int limit = Math.min(result.length(), maxLength);
    return result.substring(0, limit);
  }
}
