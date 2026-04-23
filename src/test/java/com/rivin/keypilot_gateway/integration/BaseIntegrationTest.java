package com.rivin.keypilot_gateway.integration;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public abstract class BaseIntegrationTest {

    protected static final WireMockServer wireMockServer;

    static {
        wireMockServer = new WireMockServer(
                WireMockConfiguration.wireMockConfig().dynamicPort()
        );
        wireMockServer.start();

        // Ensure WireMock shuts down with the JVM
        Runtime.getRuntime().addShutdownHook(
                new Thread(wireMockServer::stop)
        );
    }

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        String baseUrl = wireMockServer.baseUrl();
        registry.add("keypilot.providers.openai.base-url",    () -> baseUrl);
        registry.add("keypilot.providers.anthropic.base-url", () -> baseUrl);
        registry.add("keypilot.providers.gemini.base-url",    () -> baseUrl);
        registry.add("keypilot.storage.path",
                () -> System.getProperty("java.io.tmpdir") + "/gateway-it-keys.json");
        registry.add("keypilot.retry.max-attempts", () -> "3");
    }
}