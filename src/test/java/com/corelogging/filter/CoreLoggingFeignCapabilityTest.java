package com.corelogging.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import feign.Client;
import feign.Request;
import feign.Response;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

class CoreLoggingFeignCapabilityTest {

  private CoreLoggingFeignCapability capability;
  private Client mockClient;
  private Request request;
  private Response response;

  @BeforeEach
  void setUp() {
    capability = new CoreLoggingFeignCapability();
    mockClient = mock(Client.class);
    request =
        Request.create(
            Request.HttpMethod.GET,
            "http://api.test/feign",
            Collections.emptyMap(),
            null,
            StandardCharsets.UTF_8,
            null);
    response =
        Response.builder()
            .status(200)
            .reason("OK")
            .request(request)
            .headers(Collections.emptyMap())
            .build();
    MDC.clear();
    TestAppender.clear();
  }

  @Test
  void shouldLogAndPopulateMdcForSuccessfulRequest() throws IOException {
    when(mockClient.execute(any(), any())).thenReturn(response);

    Client enrichedClient = capability.enrich(mockClient);
    Response actualResponse = enrichedClient.execute(request, new Request.Options());

    assertThat(actualResponse).isEqualTo(response);
    assertThat(MDC.get("log_type")).isNull();

    assertThat(TestAppender.events).isNotEmpty();
    ch.qos.logback.classic.spi.ILoggingEvent event = TestAppender.events.get(0);
    assertThat(event.getMDCPropertyMap().get("log_type")).isEqualTo("out_request");
    assertThat(event.getMDCPropertyMap().get("span.kind")).isEqualTo("CLIENT");
    assertThat(event.getMDCPropertyMap().get("http.method")).isEqualTo("GET");
    assertThat(event.getMDCPropertyMap().get("http.url")).isEqualTo("http://api.test/feign");
    assertThat(event.getMDCPropertyMap().get("http.status_code")).isEqualTo("200");
    assertThat(event.getMDCPropertyMap().get("http.duration_ms")).isNotNull();
    assertThat(Integer.parseInt(event.getMDCPropertyMap().get("http.duration_ms")))
        .isGreaterThanOrEqualTo(0);
  }

  @Test
  void shouldLogAndPopulateMdcForFailedRequest() throws IOException {
    IOException ioException = new IOException("Connection refused");
    when(mockClient.execute(any(), any())).thenThrow(ioException);

    Client enrichedClient = capability.enrich(mockClient);

    assertThatThrownBy(() -> enrichedClient.execute(request, new Request.Options()))
        .isInstanceOf(IOException.class)
        .hasMessage("Connection refused");

    assertThat(MDC.get("log_type")).isNull();

    assertThat(TestAppender.events).isNotEmpty();
    ch.qos.logback.classic.spi.ILoggingEvent event = TestAppender.events.get(0);
    assertThat(event.getMDCPropertyMap().get("log_type")).isEqualTo("out_request");
    assertThat(event.getMDCPropertyMap().get("span.kind")).isEqualTo("CLIENT");
    assertThat(event.getMDCPropertyMap().get("http.method")).isEqualTo("GET");
    assertThat(event.getMDCPropertyMap().get("http.url")).isEqualTo("http://api.test/feign");
    assertThat(event.getMDCPropertyMap().get("http.duration_ms")).isNotNull();
    assertThat(Integer.parseInt(event.getMDCPropertyMap().get("http.duration_ms")))
        .isGreaterThanOrEqualTo(0);

    assertThat(event.getMDCPropertyMap().containsKey("http.status_code")).isFalse();

    assertThat(event.getMDCPropertyMap().get("error.stacktrace")).contains("Connection refused");
  }
}
