package com.corelogging.filter;

import com.corelogging.config.CoreLoggingProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

public class CoreLoggingFilter extends OncePerRequestFilter implements Ordered {

  private static final Logger log = LoggerFactory.getLogger(CoreLoggingFilter.class);
  private final CoreLoggingProperties properties;

  public CoreLoggingFilter(CoreLoggingProperties properties) {
    this.properties = properties;
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

    Throwable unhandledException = null;
    try {

      MDC.put("log_type", "in_request");
      MDC.put("span.kind", "SERVER");
      MDC.put("http.method", request.getMethod());
      MDC.put("http.url", request.getRequestURI());

      filterChain.doFilter(requestToUse, responseToUse);

    } catch (Throwable t) {
      unhandledException = t;
      throw t;
    } finally {

      long duration = System.currentTimeMillis() - startTime;
      int status = responseToUse.getStatus();

      if (unhandledException != null) {

        status = 500;
        java.io.StringWriter sw = new java.io.StringWriter();
        unhandledException.printStackTrace(new java.io.PrintWriter(sw));
        MDC.put("error.stacktrace", sw.toString());
      }

      MDC.put("http.status_code", String.valueOf(status));
      MDC.put("http.duration_ms", String.valueOf(duration));

      if (wrapPayload) {
        logPayload(
            (ContentCachingRequestWrapper) requestToUse,
            (ContentCachingResponseWrapper) responseToUse);

        ((ContentCachingResponseWrapper) responseToUse).copyBodyToResponse();
      }

      if (unhandledException != null) {
        log.error(
            "Failed processing incoming request {} {}",
            request.getMethod(),
            request.getRequestURI(),
            unhandledException);
      } else {
        log.info("Processed incoming request {} {}", request.getMethod(), request.getRequestURI());
      }

      MDC.remove("log_type");
      MDC.remove("span.kind");
      MDC.remove("http.method");
      MDC.remove("http.url");
      MDC.remove("http.status_code");
      MDC.remove("http.duration_ms");
      MDC.remove("error.stacktrace");
      MDC.remove("http.request.body");
      MDC.remove("http.response.body");
    }
  }

  private void logPayload(
      ContentCachingRequestWrapper request, ContentCachingResponseWrapper response) {

    byte[] requestBody = request.getContentAsByteArray();
    if (requestBody.length > 0) {
      MDC.put("http.request.body", new String(requestBody));
    }

    byte[] responseBody = response.getContentAsByteArray();
    if (responseBody.length > 0) {
      MDC.put("http.response.body", new String(responseBody));
    }
  }

  @Override
  public int getOrder() {

    return Ordered.LOWEST_PRECEDENCE - 10;
  }
}
