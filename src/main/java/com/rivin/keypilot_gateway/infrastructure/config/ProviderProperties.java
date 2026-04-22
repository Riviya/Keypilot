package com.rivin.keypilot_gateway.infrastructure.config;

import com.rivin.keypilot_gateway.infrastructure.proxy.auth.AuthType;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;


@ConfigurationProperties(prefix = "keypilot")
public class ProviderProperties {

    private Map<String, ProviderDefinition> providers = new HashMap<>();
    private String defaultProvider = "openai";

    public Map<String, ProviderDefinition> getProviders() {
        return providers;
    }

    public void setProviders(Map<String, ProviderDefinition> providers) {
        this.providers = providers;
    }

    public String getDefaultProvider() {
        return defaultProvider;
    }

    public void setDefaultProvider(String defaultProvider) {
        this.defaultProvider = defaultProvider;
    }

    // Represents one provider
    public static class ProviderDefinition {
        private String baseUrl;
        private AuthType authType = AuthType.BEARER;
        private String authParamName;

        public String getBaseUrl() {
            return baseUrl; }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl; }

        public AuthType getAuthType() {
            return authType; }

        public void setAuthType(AuthType authType) {
            this.authType = authType;
        }

        public String getAuthParamName() {
            return authParamName;
        }

        public void setAuthParamName(String authParamName) {
            this.authParamName = authParamName;
        }

    }
}
