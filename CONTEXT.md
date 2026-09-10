# Domain Glossary

- **Correlation ID**: O identificador que amarra uma única transação distribuída através de múltiplos serviços. Pode chegar em vários formatos de headers legados (ex: `x-correlation-id`, `x-request-id`, `traceparent`).
- **Trace Context**: O escopo de observabilidade ativo que representa uma unidade de trabalho.
- **Trace Manager**: O coordenador central que gerencia o ciclo de vida do `Trace Context`. Ele abraça a execução (inversão de controle), garantindo que o MDC seja populado, durações sejam calculadas, erros sejam capturados e os logs sejam emitidos de forma consistente, independente do transporte.
- **Correlation Propagator**: A interface (baseada no `io.micrometer.tracing.propagation.Propagator`) responsável por extrair e injetar Correlation IDs nos transportes, servindo de fundação para a rastreabilidade distribuída.
- **Payload Auditor**: A camada de privacidade e compliance responsável por realizar cache seguro, leitura e ofuscação de dados sensíveis de payloads brutos (bodies HTTP, mensagens Kafka) antes que sejam logados.
