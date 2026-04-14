package com.rivin.keypilot_gateway.infrastructure.proxy;

import com.rivin.keypilot_gateway.domain.exception.ProviderCommunicationException;
import com.rivin.keypilot_gateway.domain.proxy.ProviderGateway;
import com.rivin.keypilot_gateway.domain.proxy.ProxyRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class OpenAiProviderGateway implements ProviderGateway {

    private static final String OPENAI_BASE_URL = "https://api.openai.com";

    private final RestClient restClient;

    // Default Constructor
    public OpenAiProviderGateway() {
        this.restClient = RestClient.builder().baseUrl(OPENAI_BASE_URL).build();
    }

    // Constructor for testing — allows injecting a mock RestClient
    OpenAiProviderGateway(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public ResponseEntity<String> forward(ProxyRequest request) {
        try {
            return restClient.method(request.method())                        // Methods: GET, POST, DELETE, PUT
                    .uri(request.path())                                      // Request path (Without the baseURL)
                    .header("Authorization", "Bearer " + request.apiKey())
                    .header("Content-Type", "application/json")
                    .headers(httpHeaders ->
                            request.headers().forEach(httpHeaders::add)        // Loop through all the restv of the headers and add them
                    )
                    .body(request.body() != null ? request.body() : "")        // Get the body if the body is null return null
                    .retrieve()                                                // Send Request
                    .toEntity(String.class);                                   // Save request as a String

        } catch (RestClientException ex) {
            throw new ProviderCommunicationException(
                    "Failed to reach OpenAI: " + ex.getMessage(), ex
            );
        }
    }
}