package com.corelogging.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;

class CoreLoggingClientInterceptorTest {

  private CoreLoggingClientInterceptor interceptor;
  private HttpRequest request;
  private byte[] body;
  private ClientHttpRequestExecution execution;
  private ClientHttpResponse response;

  @BeforeEach
  void setUp() {
    interceptor = new CoreLoggingClientInterceptor();
    request = mock(HttpRequest.class);
    body = "request body".getBytes();
    execution = mock(ClientHttpRequestExecution.class);
    response = mock(ClientHttpResponse.class);
    MDC.clear();
    TestAppender.clear();
  }

  @Test
  void shouldLogAndPopulateMdcForOutgoingRequest() throws IOException {
    given(request.getMethod()).willReturn(HttpMethod.POST);
    given(request.getURI()).willReturn(URI.create("http://api.exemplo.com/test"));
    given(response.getStatusCode()).willReturn(org.springframework.http.HttpStatus.OK);

    Map<String, String> mdcDuringRequest = new HashMap<>();
    given(execution.execute(any(), any()))
        .willAnswer(
            invocation -> {
              mdcDuringRequest.put("correlation_id", MDC.get("correlation_id"));
              mdcDuringRequest.put("log_type", MDC.get("log_type"));
              mdcDuringRequest.put("span.kind", MDC.get("span.kind"));
              return response;
            });

    ClientHttpResponse actualResponse = interceptor.intercept(request, body, execution);

    assertThat(actualResponse).isEqualTo(response);
    assertThat(mdcDuringRequest.get("log_type")).isEqualTo("out_request");
    assertThat(mdcDuringRequest.get("span.kind")).isEqualTo("CLIENT");
    assertThat(MDC.get("log_type")).isNull();

    assertThat(TestAppender.events).isNotEmpty();
    ch.qos.logback.classic.spi.ILoggingEvent event = TestAppender.events.get(0);
    assertThat(event.getMDCPropertyMap().get("http.status_code")).isEqualTo("200");
    assertThat(event.getMDCPropertyMap().get("http.method")).isEqualTo("POST");
    assertThat(event.getMDCPropertyMap().get("http.duration_ms")).isNotNull();
    assertThat(Integer.parseInt(event.getMDCPropertyMap().get("http.duration_ms")))
        .isGreaterThanOrEqualTo(0);
  }

  @Test
  void shouldHandleNullMethodSafely() throws IOException {
    given(request.getMethod()).willReturn(null);
    given(request.getURI()).willReturn(URI.create("http://api.exemplo.com/test"));
    given(response.getStatusCode()).willReturn(org.springframework.http.HttpStatus.BAD_REQUEST);

    Map<String, String> mdcDuringRequest = new HashMap<>();
    given(execution.execute(any(), any()))
        .willAnswer(
            invocation -> {
              mdcDuringRequest.put("log_type", MDC.get("log_type"));
              return response;
            });

    interceptor.intercept(request, body, execution);

    assertThat(mdcDuringRequest.get("log_type")).isEqualTo("out_request");
  }

  @Test
  void shouldPropagateExceptionAndClearMdc() throws IOException {
    given(request.getMethod()).willReturn(HttpMethod.GET);
    given(request.getURI()).willReturn(URI.create("http://api.exemplo.com/test"));
    given(execution.execute(any(), any())).willThrow(new IOException("Timeout"));

    try {
      interceptor.intercept(request, body, execution);
    } catch (IOException e) {

    }

    assertThat(MDC.get("log_type")).isNull();

    assertThat(TestAppender.events).isNotEmpty();
    ch.qos.logback.classic.spi.ILoggingEvent event = TestAppender.events.get(0);
    assertThat(event.getMDCPropertyMap().get("error.stacktrace")).contains("Timeout");
    assertThat(event.getLevel().toString()).isEqualTo("ERROR");
    assertThat(event.getFormattedMessage()).startsWith("Failed outgoing request");
  }

  @Test
  void shouldRestorePreviousMdcState() throws IOException {
    MDC.put("log_type", "previous_log_type");
    MDC.put("span.kind", "previous_span_kind");

    given(request.getMethod()).willReturn(HttpMethod.POST);
    given(request.getURI()).willReturn(URI.create("http://api.exemplo.com/test"));
    given(response.getStatusCode()).willReturn(org.springframework.http.HttpStatus.OK);
    given(execution.execute(any(), any())).willReturn(response);

    interceptor.intercept(request, body, execution);

    assertThat(MDC.get("log_type")).isEqualTo("previous_log_type");
    assertThat(MDC.get("span.kind")).isEqualTo("previous_span_kind");

    MDC.clear();
  }
}
