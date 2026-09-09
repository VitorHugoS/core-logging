package com.corelogging.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.util.List;

@ConfigurationProperties(prefix = "core-logging")
public class CoreLoggingProperties {
    
    private Payload payload = new Payload();

    public Payload getPayload() {
        return payload;
    }

    public void setPayload(Payload payload) {
        this.payload = payload;
    }

    public static class Payload {
        private boolean enabled = false;
        private List<String> obfuscateFields = List.of("password", "token", "cpf", "document");

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public List<String> getObfuscateFields() {
            return obfuscateFields;
        }

        public void setObfuscateFields(List<String> obfuscateFields) {
            this.obfuscateFields = obfuscateFields;
        }
    }
}
