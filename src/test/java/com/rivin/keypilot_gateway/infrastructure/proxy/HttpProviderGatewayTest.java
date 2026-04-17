package com.rivin.keypilot_gateway.infrastructure.proxy;


import com.rivin.keypilot_gateway.infrastructure.proxy.auth.ProviderAuthStrategyFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


import static org.assertj.core.api.Assertions.*;

class HttpProviderGatewayTest {

    private HttpProviderGateway gateway;
    private ProviderAuthStrategyFactory authStrategyFactory;

    @BeforeEach
    void setUp() {
        gateway = new HttpProviderGateway(authStrategyFactory);
    }

    // We test the gateway using a real HTTP server (WireMock-style)
    // but since we want zero extra deps, we use httptest pattern via
    // MockRestServiceServer from Spring Testz

    @Test
    void shouldBuildCorrectTargetUrlFromBaseUrlAndPath() {
        // Structural test — verify URL construction logic

        String result = gateway.buildTargetUrl(
                "https://api.openai.com",
                "/v1/chat/completions"
        );

        assertThat(result).isEqualTo("https://api.openai.com/v1/chat/completions");
    }

    @Test
    void shouldBuildUrlWithTrailingSlashOnBaseUrl() {

        String result = gateway.buildTargetUrl(
                "https://api.openai.com/",
                "/v1/chat/completions"
        );

        // Must not produce double slash
        assertThat(result).isEqualTo("https://api.openai.com/v1/chat/completions");
        assertThat(result).doesNotContain("//v1");
    }

    @Test
    void shouldBuildUrlWithoutLeadingSlashOnPath() {

        String result = gateway.buildTargetUrl(
                "https://api.openai.com",
                "v1/chat/completions"     // no leading slash
        );

        assertThat(result).isEqualTo("https://api.openai.com/v1/chat/completions");
    }
}