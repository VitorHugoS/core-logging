package com.corelogging.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "core-logging")
public class CoreLoggingProperties {

  private Payload payload = new Payload();

  public Payload getPayload() {
    return payload;
  }

  public void setPayload(Payload payload) {
    this.payload = payload;
  }

  public static class Payload {
    private boolean enabled = false;
    private int maxCacheSize = 1048576;
    private List<String> obfuscateFields = List.of("password", "token", "cpf", "document");

    public int getMaxCacheSize() {
      return maxCacheSize;
    }

    public void setMaxCacheSize(int maxCacheSize) {
      this.maxCacheSize = maxCacheSize;
    }

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    public List<String> getObfuscateFields() {
      return obfuscateFields;
    }

    public void setObfuscateFields(List<String> obfuscateFields) {
      this.obfuscateFields = obfuscateFields;
    }
  }
}
