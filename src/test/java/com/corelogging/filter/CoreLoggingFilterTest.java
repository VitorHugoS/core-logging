package com.corelogging.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.corelogging.config.CoreLoggingProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

class CoreLoggingFilterTest {

  private CoreLoggingProperties properties;
  private CoreLoggingFilter filter;
  private MockHttpServletRequest request;
  private MockHttpServletResponse response;
  private FilterChain filterChain;

  @BeforeEach
  void setUp() {
    properties = new CoreLoggingProperties();
    filter = new CoreLoggingFilter(properties);
    request = new MockHttpServletRequest();
    request.setMethod("POST");
    request.setRequestURI("/api/test");
    response = new MockHttpServletResponse();
    filterChain = mock(FilterChain.class);
    MDC.clear();
    TestAppender.clear();
  }

  @Test
  void shouldLogAndPopulateMdcForIncomingRequest() throws ServletException, IOException {

    final Map<String, String>[] mdcDuringRequest = new Map[1];

    doAnswer(
            invocation -> {
              mdcDuringRequest[0] = MDC.getCopyOfContextMap();
              return null;
            })
        .when(filterChain)
        .doFilter(any(), any());

    filter.doFilter(request, response, filterChain);

    assertThat(mdcDuringRequest[0].get("log_type")).isEqualTo("in_request");
    assertThat(mdcDuringRequest[0].get("span.kind")).isEqualTo("SERVER");

    assertThat(MDC.get("log_type")).isNull();

    assertThat(TestAppender.events).isNotEmpty();
    ch.qos.logback.classic.spi.ILoggingEvent event = TestAppender.events.get(0);
    assertThat(event.getMDCPropertyMap().get("http.status_code")).isEqualTo("200");
    assertThat(event.getMDCPropertyMap().get("http.method")).isEqualTo("POST");
    assertThat(event.getMDCPropertyMap().get("http.url")).isEqualTo("/api/test");
    assertThat(event.getMDCPropertyMap().get("http.duration_ms")).isNotNull();
    assertThat(Integer.parseInt(event.getMDCPropertyMap().get("http.duration_ms")))
        .isGreaterThanOrEqualTo(0);
  }

  @Test
  void shouldWrapOnlyIfNotAlreadyWrapped() throws ServletException, IOException {
    ContentCachingRequestWrapper wrappedReq = new ContentCachingRequestWrapper(request, 1024);
    ContentCachingResponseWrapper wrappedRes = new ContentCachingResponseWrapper(response);

    filter.doFilter(wrappedReq, wrappedRes, filterChain);

    verify(filterChain, times(1)).doFilter(wrappedReq, wrappedRes);
  }

  @Test
  void shouldLogPayloadIfEnabledAndNotEmpty() throws ServletException, IOException {
    properties.getPayload().setEnabled(true);
    request.setContent("request content".getBytes());

    doAnswer(
            invocation -> {
              ContentCachingRequestWrapper req = invocation.getArgument(0);
              req.getInputStream().readAllBytes();
              ContentCachingResponseWrapper res = invocation.getArgument(1);
              res.getWriter().write("response content");
              res.getWriter().flush();
              return null;
            })
        .when(filterChain)
        .doFilter(any(), any());

    filter.doFilter(request, response, filterChain);

    assertThat(response.getContentAsByteArray()).isNotEmpty();
    assertThat(new String(response.getContentAsByteArray())).isEqualTo("response content");

    assertThat(TestAppender.events).isNotEmpty();
    ch.qos.logback.classic.spi.ILoggingEvent event = TestAppender.events.get(0);
    assertThat(event.getMDCPropertyMap().get("http.request.body")).isEqualTo("request content");
    assertThat(event.getMDCPropertyMap().get("http.response.body")).isEqualTo("response content");
  }

  @Test
  void shouldNotLogPayloadIfEmpty() throws ServletException, IOException {
    properties.getPayload().setEnabled(true);
    request.setContent(new byte[0]);

    filter.doFilter(request, response, filterChain);

    assertThat(TestAppender.events).isNotEmpty();
    ch.qos.logback.classic.spi.ILoggingEvent event = TestAppender.events.get(0);
    assertThat(event.getMDCPropertyMap().containsKey("http.request.body")).isFalse();
    assertThat(event.getMDCPropertyMap().containsKey("http.response.body")).isFalse();
  }

  @Test
  void shouldReturnOrder() {
    assertThat(filter.getOrder())
        .isEqualTo(org.springframework.core.Ordered.LOWEST_PRECEDENCE - 10);
  }

  @Test
  void shouldLogExceptionAndPopulateMdcStacktrace() throws ServletException, IOException {
    RuntimeException exception = new RuntimeException("Unexpected error");
    doAnswer(
            invocation -> {
              throw exception;
            })
        .when(filterChain)
        .doFilter(any(), any());

    try {
      filter.doFilter(request, response, filterChain);
    } catch (Exception e) {
      assertThat(e).isEqualTo(exception);
    }

    assertThat(TestAppender.events).isNotEmpty();
    ch.qos.logback.classic.spi.ILoggingEvent event = TestAppender.events.get(0);
    assertThat(event.getMDCPropertyMap().get("http.status_code")).isEqualTo("500");
    assertThat(event.getMDCPropertyMap().get("error.stacktrace")).contains("Unexpected error");
    assertThat(event.getLevel().toString()).isEqualTo("ERROR");
  }

  @Test
  void shouldNotWrapIfAlreadyWrapped() throws Exception {
    properties.getPayload().setEnabled(true);
    properties.getPayload().setMaxCacheSize(1024);

    ContentCachingRequestWrapper wrappedReq = new ContentCachingRequestWrapper(request, 1024);
    ContentCachingResponseWrapper wrappedRes = new ContentCachingResponseWrapper(response);

    filter.doFilterInternal(wrappedReq, wrappedRes, filterChain);

    verify(filterChain).doFilter(wrappedReq, wrappedRes);
  }
}
