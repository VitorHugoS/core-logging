package com.corelogging.filter;

import com.corelogging.config.CoreLoggingProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

public class CoreLoggingFilter extends OncePerRequestFilter implements Ordered {

  private static final Logger log = LoggerFactory.getLogger(CoreLoggingFilter.class);
  private final CoreLoggingProperties properties;
  private final PayloadObfuscator obfuscator;

  public CoreLoggingFilter(CoreLoggingProperties properties) {
    this.properties = properties;
    this.obfuscator =
        new PayloadObfuscator(
            properties.getPayload().getObfuscateFields(), properties.getPayload().getMaxLength());
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    long startTime = System.currentTimeMillis();
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

    try (var scope = com.corelogging.scope.ObservabilityScope.start(log, "in_request", "SERVER")) {
      scope.tag("http.method", request.getMethod());
      scope.tag("http.url", request.getRequestURI());

      try {
        filterChain.doFilter(requestToUse, responseToUse);
      } catch (Throwable t) {
        scope.recordError(t);
        throw t;
      } finally {
        int status = responseToUse.getStatus();
        if (scope.hasError()) {
          if (status == 200) {
            status = 500;
          }
        }
        scope.tag("http.status_code", String.valueOf(status));
        scope.computeDurationAs("http.duration_ms");

        if (wrapPayload) {
          logPayload(
              (ContentCachingRequestWrapper) requestToUse,
              (ContentCachingResponseWrapper) responseToUse,
              scope);
          ((ContentCachingResponseWrapper) responseToUse).copyBodyToResponse();
        }

        if (scope.hasError()) {
          scope.closeWith(
              "Failed processing incoming request {} {}",
              request.getMethod(),
              request.getRequestURI());
        } else {
          scope.closeWith(
              "Processed incoming request {} {}", request.getMethod(), request.getRequestURI());
        }
      }
    }
  }

  private void logPayload(
      ContentCachingRequestWrapper request,
      ContentCachingResponseWrapper response,
      com.corelogging.scope.ObservabilityScope scope) {

    byte[] requestBody = request.getContentAsByteArray();
    if (requestBody.length > 0) {
      scope.tag("http.request.body", obfuscator.process(new String(requestBody)));
    }

    byte[] responseBody = response.getContentAsByteArray();
    if (responseBody.length > 0) {
      scope.tag("http.response.body", obfuscator.process(new String(responseBody)));
    }
  }

  @Override
  public int getOrder() {

    return Ordered.LOWEST_PRECEDENCE - 10;
  }
}
