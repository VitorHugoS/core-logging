package com.corelogging.auditor;

import com.corelogging.config.CoreLoggingProperties;
import com.corelogging.filter.PayloadObfuscator;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

public class DefaultHttpPayloadAuditor implements HttpPayloadAuditor {

  private final CoreLoggingProperties properties;
  private final PayloadObfuscator obfuscator;

  public DefaultHttpPayloadAuditor(CoreLoggingProperties properties) {
    this.properties = properties;
    this.obfuscator =
        new PayloadObfuscator(
            properties.getPayload().getObfuscateFields(), properties.getPayload().getMaxLength());
  }

  public void auditAndProceed(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws Exception {

    boolean wrapPayload = properties.getPayload().isEnabled();

    HttpServletRequest requestToUse = request;
    HttpServletResponse responseToUse = response;

    if (wrapPayload) {
      if (!(request instanceof ContentCachingRequestWrapper)) {
        requestToUse =
            new ContentCachingRequestWrapper(request, properties.getPayload().getMaxCacheSize());
      }
      if (!(response instanceof ContentCachingResponseWrapper)) {
        responseToUse = new ContentCachingResponseWrapper(response);
      }
    }

    // Proceed down the chain
    filterChain.doFilter(requestToUse, responseToUse);

    // After chain, audit payloads if needed
    if (wrapPayload) {
      logPayload(
          (ContentCachingRequestWrapper) requestToUse,
          (ContentCachingResponseWrapper) responseToUse);
      ((ContentCachingResponseWrapper) responseToUse).copyBodyToResponse();
    }
  }

  private void logPayload(
      ContentCachingRequestWrapper request, ContentCachingResponseWrapper response) {
    byte[] requestBody = request.getContentAsByteArray();
    if (requestBody.length > 0) {
      MDC.put("http.request.body", obfuscator.process(new String(requestBody)));
    }

    byte[] responseBody = response.getContentAsByteArray();
    if (responseBody.length > 0) {
      MDC.put("http.response.body", obfuscator.process(new String(responseBody)));
    }
  }
}
