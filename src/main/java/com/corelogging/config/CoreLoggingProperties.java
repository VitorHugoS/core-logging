package com.corelogging.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "core-logging")
public class CoreLoggingProperties {

  private Payload payload = new Payload();

  /** HTTP header and MDC key name for correlation ID */
  /** List of accepted correlation ID headers for incoming requests */
  private List<String> acceptedCorrelationIdHeaders =
      List.of("x-correlation-id", "x-request-id", "correlation-id", "traceparent", "b3");

  private String correlationIdHeader = "x-correlation-id";

  public List<String> getAcceptedCorrelationIdHeaders() {
    return acceptedCorrelationIdHeaders;
  }

  public void setAcceptedCorrelationIdHeaders(List<String> acceptedCorrelationIdHeaders) {
    this.acceptedCorrelationIdHeaders = acceptedCorrelationIdHeaders;
  }

  public String getCorrelationIdHeader() {
    return correlationIdHeader;
  }

  public void setCorrelationIdHeader(String correlationIdHeader) {
    this.correlationIdHeader = correlationIdHeader;
  }

  public Payload getPayload() {
    return payload;
  }

  public void setPayload(Payload payload) {
    this.payload = payload;
  }

  public static class Payload {
    private boolean enabled = false;
    private int maxCacheSize = 1048576;
    private int maxLength = 10000;
    private List<String> obfuscateFields = List.of("password", "token", "cpf", "document");

    public int getMaxLength() {
      return maxLength;
    }

    public void setMaxLength(int maxLength) {
      this.maxLength = maxLength;
    }

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
