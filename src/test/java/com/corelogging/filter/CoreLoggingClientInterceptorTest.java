package com.corelogging.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.net.URI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.slf4j.MDC;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;

class CoreLoggingClientInterceptorTest {

  private CoreLoggingClientInterceptor interceptor;
  private HttpRequest request;
  private ClientHttpRequestExecution execution;
  private ClientHttpResponse response;

  @BeforeEach
  void setUp() {
    interceptor = new CoreLoggingClientInterceptor();
    request = mock(HttpRequest.class);
    execution = mock(ClientHttpRequestExecution.class);
    response = mock(ClientHttpResponse.class);
  }

  @Test
  void shouldInterceptAndLogCorrectly() throws IOException {
    when(request.getMethod()).thenReturn(HttpMethod.POST);
    when(request.getURI()).thenReturn(URI.create("http://api.exemplo.com/test"));
    when(execution.execute(any(HttpRequest.class), any(byte[].class))).thenReturn(response);
    when(response.getStatusCode()).thenReturn(HttpStatus.OK);
    byte[] body = "test_body".getBytes();

    try (MockedStatic<MDC> mockedMdc = mockStatic(MDC.class)) {
      mockedMdc.when(() -> MDC.get(anyString())).thenReturn(null);

      ClientHttpResponse actualResponse = interceptor.intercept(request, body, execution);
      assertThat(actualResponse).isEqualTo(response);

      mockedMdc.verify(() -> MDC.put("log_type", "out_request"));
      mockedMdc.verify(() -> MDC.put("span.kind", "CLIENT"));
      mockedMdc.verify(() -> MDC.put("http.method", "POST"));
      mockedMdc.verify(() -> MDC.put("http.url", "http://api.exemplo.com/test"));
      mockedMdc.verify(() -> MDC.put("http.status_code", "200"));
      mockedMdc.verify(() -> MDC.remove("log_type"));

      verify(execution, times(1)).execute(request, body);
    }
  }

  @Test
  void shouldRestorePreviousMdcState() throws IOException {
    when(request.getMethod()).thenReturn(HttpMethod.GET);
    when(request.getURI()).thenReturn(URI.create("http://api.exemplo.com/test"));
    when(execution.execute(any(HttpRequest.class), any(byte[].class))).thenReturn(response);
    when(response.getStatusCode()).thenReturn(HttpStatus.NOT_FOUND);

    try (MockedStatic<MDC> mockedMdc = mockStatic(MDC.class)) {
      mockedMdc.when(() -> MDC.get("log_type")).thenReturn("in_request");
      mockedMdc.when(() -> MDC.get("span.kind")).thenReturn("SERVER");

      interceptor.intercept(request, new byte[0], execution);

      mockedMdc.verify(() -> MDC.put("log_type", "in_request"));
      mockedMdc.verify(() -> MDC.put("span.kind", "SERVER"));
    }
  }

  @Test
  void shouldHandleNullMethod() throws IOException {
    when(request.getMethod()).thenReturn(null);
    when(request.getURI()).thenReturn(URI.create("http://api.exemplo.com/test"));
    when(execution.execute(any(HttpRequest.class), any(byte[].class))).thenReturn(response);
    when(response.getStatusCode()).thenReturn(HttpStatus.OK);

    try (MockedStatic<MDC> mockedMdc = mockStatic(MDC.class)) {
      mockedMdc.when(() -> MDC.get(anyString())).thenReturn(null);

      interceptor.intercept(request, new byte[0], execution);

      mockedMdc.verify(() -> MDC.put("log_type", "out_request"));
      // Shouldn't call MDC.put("http.method", ...) because it's null
      mockedMdc.verify(() -> MDC.put(eq("http.method"), anyString()), never());
    }
  }
}
