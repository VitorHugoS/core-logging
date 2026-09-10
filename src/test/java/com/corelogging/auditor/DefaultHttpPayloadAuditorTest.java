package com.corelogging.auditor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import com.corelogging.config.CoreLoggingProperties;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

class DefaultHttpPayloadAuditorTest {

  private CoreLoggingProperties properties;
  private DefaultHttpPayloadAuditor auditor;
  private MockHttpServletRequest request;
  private MockHttpServletResponse response;
  private FilterChain filterChain;

  @BeforeEach
  void setUp() {
    properties = new CoreLoggingProperties();
    auditor = new DefaultHttpPayloadAuditor(properties);
    request = new MockHttpServletRequest();
    response = new MockHttpServletResponse();
    filterChain = mock(FilterChain.class);
    MDC.clear();
  }

  @AfterEach
  void tearDown() {
    MDC.clear();
  }

  @Test
  void testAuditDisabled() throws Exception {
    properties.getPayload().setEnabled(false);

    auditor.auditAndProceed(request, response, filterChain);

    assertThat(MDC.get("http.request.body")).isNull();
    assertThat(MDC.get("http.response.body")).isNull();
  }

  @Test
  void testAuditEnabledWithPayload() throws Exception {
    properties.getPayload().setEnabled(true);
    request.setContent("req body".getBytes());

    doAnswer(
            invocation -> {
              ContentCachingRequestWrapper req = invocation.getArgument(0);
              req.getInputStream().readAllBytes(); // consume
              ContentCachingResponseWrapper res = invocation.getArgument(1);
              res.getWriter().write("res body");
              res.getWriter().flush();
              return null;
            })
        .when(filterChain)
        .doFilter(any(), any());

    auditor.auditAndProceed(request, response, filterChain);

    assertThat(MDC.get("http.request.body")).isEqualTo("req body");
    assertThat(MDC.get("http.response.body")).isEqualTo("res body");
  }

  @Test
  void testAuditEnabledAlreadyWrapped() throws Exception {
    properties.getPayload().setEnabled(true);
    ContentCachingRequestWrapper wrappedReq = new ContentCachingRequestWrapper(request, 1024);
    ContentCachingResponseWrapper wrappedRes = new ContentCachingResponseWrapper(response);

    auditor.auditAndProceed(wrappedReq, wrappedRes, filterChain);

    assertThat(MDC.get("http.request.body")).isNull();
  }
}
