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

/**
 * Filtro que substitui o Aspecto (@Around). Captura toda a requisição, independentemente de erros
 * de conversão no DispatcherServlet.
 */
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

    // Fazemos o cache do stream para poder ler o body sem consumir o fluxo do InputStream
    if (wrapPayload) {
      if (!(request instanceof ContentCachingRequestWrapper)) {
        requestToUse = new ContentCachingRequestWrapper(request);
      }
      if (!(response instanceof ContentCachingResponseWrapper)) {
        responseToUse = new ContentCachingResponseWrapper(response);
      }
    }

    try {
      // Setup metadados iniciais no MDC
      MDC.put("log_type", "in_request");
      MDC.put("span.kind", "SERVER");
      MDC.put("http.method", request.getMethod());
      MDC.put("http.url", request.getRequestURI());

      // Continua a cadeia de filtros e chega no Controller
      filterChain.doFilter(requestToUse, responseToUse);

    } finally {
      // O finally garante que sempre vamos logar a saída, mesmo que estoure uma Exception bruta
      long duration = System.currentTimeMillis() - startTime;
      int status = responseToUse.getStatus();

      MDC.put("http.status_code", String.valueOf(status));
      MDC.put("http.duration_ms", String.valueOf(duration));

      // Extrai e loga payload se estiver habilitado
      if (wrapPayload) {
        logPayload(
            (ContentCachingRequestWrapper) requestToUse,
            (ContentCachingResponseWrapper) responseToUse);

        // IMPORTANTÍSSIMO: Copiar o body cachead de volta para o output stream
        ((ContentCachingResponseWrapper) responseToUse).copyBodyToResponse();
      }

      // A chamada de log única que emitirá o JSON
      log.info("Processed incoming request {} {}", request.getMethod(), request.getRequestURI());

      // Limpa o MDC para não sujar threads reutilizadas (Thread Pool)
      MDC.remove("log_type");
      MDC.remove("span.kind");
      MDC.remove("http.method");
      MDC.remove("http.url");
      MDC.remove("http.status_code");
      MDC.remove("http.duration_ms");
      MDC.remove("http.request.body");
      MDC.remove("http.response.body");
    }
  }

  private void logPayload(
      ContentCachingRequestWrapper request, ContentCachingResponseWrapper response) {
    // Extrai o Payload. (Neste ponto da implementação, conectaremos a ofuscação LGPD via
    // Regex/Jackson)
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
    // Executa logo após o Spring Security, mas antes do DispatcherServlet
    return Ordered.LOWEST_PRECEDENCE - 10;
  }
}
