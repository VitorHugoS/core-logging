package com.corelogging.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

public class CoreLoggingClientInterceptor implements ClientHttpRequestInterceptor {

    private static final Logger log = LoggerFactory.getLogger(CoreLoggingClientInterceptor.class);

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        long startTime = System.currentTimeMillis();
        
        // Salva estado anterior do MDC caso essa thread já estivesse processando um in_request
        String previousLogType = MDC.get("log_type");
        String previousSpanKind = MDC.get("span.kind");

        try {
            MDC.put("log_type", "out_request");
            MDC.put("span.kind", "CLIENT");
            if (request.getMethod() != null) {
                MDC.put("http.method", request.getMethod().name());
            }
            MDC.put("http.url", request.getURI().toString());

            // Executa a chamada real
            ClientHttpResponse response = execution.execute(request, body);
            
            long duration = System.currentTimeMillis() - startTime;
            MDC.put("http.status_code", String.valueOf(response.getStatusCode().value()));
            MDC.put("http.duration_ms", String.valueOf(duration));
            
            log.info("Processed outgoing request {} {}", request.getMethod(), request.getURI());
            
            return response;
        } finally {
            // Restaura o MDC para o estado original (provavelmente in_request)
            if (previousLogType != null) {
                MDC.put("log_type", previousLogType);
            } else {
                MDC.remove("log_type");
            }
            
            if (previousSpanKind != null) {
                MDC.put("span.kind", previousSpanKind);
            } else {
                MDC.remove("span.kind");
            }
            
            MDC.remove("http.method");
            MDC.remove("http.url");
            MDC.remove("http.status_code");
            MDC.remove("http.duration_ms");
        }
    }
}
