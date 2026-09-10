# Core Logging Spring Boot Starter

`core-logging-spring-boot-starter` é uma biblioteca pronta para uso (*plug-and-play*) que resolve a padronização de logs estruturados (JSON), propagação de IDs de correlação e observabilidade de requisições HTTP e eventos Kafka em arquiteturas de microsserviços usando Spring Boot 4.

## Principais Features

*   **Logs Estruturados em JSON:** Saída padronizada baseada em Logback pronta para indexadores (Elasticsearch, Datadog, CloudWatch).
*   **Auto-propagação de Correlation ID:** Transita automaticamente IDs de correlação (ex: `x-correlation-id`) nas chamadas de entrada e saída.
*   **Suporte a Múltiplos Headers Legados:** Reconhece dinamicamente IDs de correlação que venham em formatos diferentes (ex: `x-request-id`, `traceparent`).
*   **Integração HTTP Transparente:**
    *   Intercepta chamadas HTTP REST de entrada (Servlet Filter).
    *   Propaga headers nas chamadas de saída via **RestTemplate** ou **OpenFeign**.
*   **Integração Kafka Nativa:** Injeta e extrai Correlation IDs de headers de mensagens via `ProducerInterceptor` e `RecordInterceptor` do Spring Kafka.
*   **Contexto em Threads Assíncronas:** Propagação nativa do contexto MDC (Mapped Diagnostic Context) em execuções `@Async`.
*   **Auditoria de Payloads e Erros:** 
    *   Logs automáticos da duração das requisições (`http.duration_ms`).
    *   Captura estruturada de Stacktraces de exceções.
    *   (Opcional) Registro do *body* (payload) de requests e responses, com mascaramento/ofuscação automática de campos sensíveis (senhas, tokens, CPF).

---

## Como Instalar

Adicione a dependência no seu `pom.xml`:

```xml
<dependency>
    <groupId>com.corelogging</groupId>
    <artifactId>core-logging-spring-boot-starter</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

Apenas adicionando a biblioteca, a auto-configuração do Spring Boot registrará todos os beans necessários.

---

## Como Utilizar no seu Código

### 1. Escrevendo Logs Estruturados Customizados

Para adicionar campos customizados no JSON final do log sem poluir o log message, utilize o `CoreLoggerFactory`:

```java
import com.corelogging.CoreLoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class MeuServico {

    private final com.corelogging.CoreLogger log;

    public MeuServico(CoreLoggerFactory loggerFactory) {
        this.log = loggerFactory.getLogger(MeuServico.class);
    }

    public void processar(Pedido pedido) {
        // Log com chaves isoladas no JSON
        log.info("Processando novo pedido")
           .with("pedido_id", pedido.getId())
           .with("valor_total", pedido.getValor())
           .log();
           
        // Log "achatando" um DTO/Objeto inteiro pro JSON
        log.info("Detalhes do comprador")
           .with(pedido.getComprador())
           .log();
    }
}
```

### 2. Chamadas HTTP de Saída (RestTemplate e Feign)

O Correlation ID recebido será passado à frente automaticamente:

*   **RestTemplate:** A biblioteca registra um `ClientHttpRequestInterceptor` padrão na aplicação. Caso você crie o seu `RestTemplate` via `RestTemplateBuilder`, ele já receberá o interceptador.
*   **OpenFeign:** A biblioteca provê um `RequestInterceptor` que será lido nativamente caso você utilize instâncias do `feign.Feign.Builder` ou `Spring Cloud OpenFeign`.

### 3. Chamadas Assíncronas (`@Async`)

O MDC será propagado automaticamente para a nova thread. Basta utilizar a anotação `@Async` nativa do Spring.

### 4. Mensageria com Kafka

*   **Consumidor (`@KafkaListener`):** Interceptado automaticamente graças à injeção no `ConcurrentKafkaListenerContainerFactory`. O correlation ID é retirado do header da mensagem.
*   **Produtor (`KafkaTemplate`):** Para habilitar o repasse de Correlation ID em mensagens publicadas, adicione a seguinte propriedade na configuração do produtor Kafka no seu projeto:
    ```yaml
    spring:
      kafka:
        producer:
          properties:
            interceptor.classes: com.corelogging.filter.CoreLoggingKafkaProducerInterceptor
    ```

---

## Configurações (`application.yml`)

A biblioteca funciona sem nenhuma configuração adicional (Zero-Config), mas oferece os seguintes parâmetros opcionais:

```yaml
core-logging:
  # Header principal gerado em respostas e repassado pro downstream
  correlation-id-header: x-correlation-id
  
  # Headers legados aceitos como Correlation ID (fallback)
  accepted-correlation-id-headers: 
    - x-correlation-id
    - x-request-id
    - correlation-id
    - traceparent
    - b3
  
  payload:
    enabled: false # Habilita o log do body de requisições/respostas HTTP
    max-cache-size: 1048576 # Tamanho máx do body cacheado (bytes)
    max-length: 10000 # Tamanho máx logado do payload (caracteres)
    # Lista de campos json cujo valor será substituído por ***
    obfuscate-fields: 
      - password
      - token
      - cpf
      - document
```
