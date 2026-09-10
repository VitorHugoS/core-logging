#!/bin/bash
sed -i '' -e 's/if (correlationId == null) {/if (correlationId == null) {\
        correlationId = org.slf4j.MDC.get("traceId");\
      }\
\
      if (correlationId == null) {/g' src/main/java/com/corelogging/filter/CoreLoggingFilter.java

sed -i '' -e 's/if (correlationId == null) {/if (correlationId == null) {\
      correlationId = org.slf4j.MDC.get("traceId");\
    }\
\
    if (correlationId == null) {/g' src/main/java/com/corelogging/filter/CoreLoggingKafkaConsumerInterceptor.java
