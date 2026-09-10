#!/bin/bash
sed -i '' -e 's/public CoreLoggerFactory coreLoggerFactory(tools.jackson.databind.ObjectMapper objectMapper) {/public CoreLoggerFactory coreLoggerFactory(tools.jackson.databind.ObjectMapper objectMapper, CoreLoggingProperties properties) {/g' src/main/java/com/corelogging/config/CoreLoggingAutoConfiguration.java
sed -i '' -e 's/return new CoreLoggerFactory(objectMapper);/return new CoreLoggerFactory(objectMapper, properties);/g' src/main/java/com/corelogging/config/CoreLoggingAutoConfiguration.java
