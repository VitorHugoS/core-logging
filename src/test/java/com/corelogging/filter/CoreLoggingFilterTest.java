package com.corelogging.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.corelogging.config.CoreLoggingProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
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
    properties.getPayload().setEnabled(false);
    filter = new CoreLoggingFilter(properties);

    request = new MockHttpServletRequest();
    response = new MockHttpServletResponse();
    filterChain = mock(FilterChain.class);
  }

  @Test
  void shouldHaveCorrectOrder() {
    assertThat(filter.getOrder()).isEqualTo(Ordered.LOWEST_PRECEDENCE - 10);
  }

  @Test
  void shouldLogWithoutPayloadCache() throws ServletException, IOException {
    request.setMethod("POST");
    request.setRequestURI("/api/test");
    response.setStatus(201);

    try (MockedStatic<MDC> mockedMdc = mockStatic(MDC.class)) {
      filter.doFilter(request, response, filterChain);

      verify(filterChain, times(1)).doFilter(request, response);

      mockedMdc.verify(() -> MDC.put("log_type", "in_request"));
      mockedMdc.verify(() -> MDC.put("span.kind", "SERVER"));
      mockedMdc.verify(() -> MDC.put("http.method", "POST"));
      mockedMdc.verify(() -> MDC.put("http.url", "/api/test"));
      mockedMdc.verify(() -> MDC.put("http.status_code", "201"));
      mockedMdc.verify(() -> MDC.remove("log_type"));
    }
  }

  @Test
  void shouldLogWithPayloadCache() throws ServletException, IOException {
    properties.getPayload().setEnabled(true);

    request.setMethod("PUT");
    request.setRequestURI("/api/data");
    request.setContent("{\"test\":\"data\"}".getBytes());
    response.setStatus(200);

    doAnswer(
            invocation -> {
              ContentCachingRequestWrapper req = invocation.getArgument(0);
              ContentCachingResponseWrapper res = invocation.getArgument(1);
              req.getInputStream().readAllBytes();
              res.getWriter().write("{\"response\":\"ok\"}");
              return null;
            })
        .when(filterChain)
        .doFilter(
            any(ContentCachingRequestWrapper.class), any(ContentCachingResponseWrapper.class));

    try (MockedStatic<MDC> mockedMdc = mockStatic(MDC.class)) {
      filter.doFilter(request, response, filterChain);

      verify(filterChain, times(1))
          .doFilter(
              any(ContentCachingRequestWrapper.class), any(ContentCachingResponseWrapper.class));

      assertThat(response.getContentAsString()).isEqualTo("{\"response\":\"ok\"}");

      mockedMdc.verify(() -> MDC.put("http.request.body", "{\"test\":\"data\"}"));
      mockedMdc.verify(() -> MDC.put("http.response.body", "{\"response\":\"ok\"}"));
    }
  }

  @Test
  void shouldWrapOnlyIfNotAlreadyWrapped() throws ServletException, IOException {
    properties.getPayload().setEnabled(true);

    ContentCachingRequestWrapper wrappedReq =
        new ContentCachingRequestWrapper(request, properties.getPayload().getMaxCacheSize());
    ContentCachingResponseWrapper wrappedRes = new ContentCachingResponseWrapper(response);

    request.setMethod("GET");
    request.setRequestURI("/api/wrap");

    try (MockedStatic<MDC> mockedMdc = mockStatic(MDC.class)) {
      filter.doFilter(wrappedReq, wrappedRes, filterChain);
      verify(filterChain, times(1)).doFilter(wrappedReq, wrappedRes);
    }
  }
}
