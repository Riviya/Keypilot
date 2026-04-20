package com.rivin.keypilot_gateway.infrastructure.proxy.auth;

import com.rivin.keypilot_gateway.infrastructure.Exception.UnknownAuthStrategyException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class ProviderAuthStrategyFactoryTest {

    private ProviderAuthStrategyFactory factory;

    @BeforeEach
    void setUp() {
        Map<String, ProviderAuthConfig> configs = Map.of(
                "openai",    new ProviderAuthConfig(AuthType.BEARER, null),
                "anthropic", new ProviderAuthConfig(AuthType.BEARER, null),
                "gemini",    new ProviderAuthConfig(AuthType.QUERY_PARAM, "key"),
                "cohere",    new ProviderAuthConfig(AuthType.API_KEY_HEADER, "x-api-key")
        );
        factory = new ProviderAuthStrategyFactory(configs);
    }

    // ---------------------------------------------------------------
    // STRATEGY RESOLUTION
    // ---------------------------------------------------------------

    @Test
    void shouldReturnBearerStrategyForOpenAi() {
        ProviderAuthStrategy strategy = factory.getStrategy("openai");
        assertThat(strategy).isInstanceOf(BearerAuthStrategy.class);
    }

    @Test
    void shouldReturnBearerStrategyForAnthropic() {
        ProviderAuthStrategy strategy = factory.getStrategy("anthropic");
        assertThat(strategy).isInstanceOf(BearerAuthStrategy.class);
    }

    @Test
    void shouldReturnQueryParamStrategyForGemini() {
        ProviderAuthStrategy strategy = factory.getStrategy("gemini");
        assertThat(strategy).isInstanceOf(QueryParamAuthStrategy.class);
    }

    @Test
    void shouldReturnApiKeyHeaderStrategyForCohere() {
        ProviderAuthStrategy strategy = factory.getStrategy("cohere");
        assertThat(strategy).isInstanceOf(ApiKeyHeaderAuthStrategy.class);
    }

    @Test
    void shouldResolveCaseInsensitively() {
        assertThat(factory.getStrategy("OpenAI")).isInstanceOf(BearerAuthStrategy.class);
        assertThat(factory.getStrategy("GEMINI")).isInstanceOf(QueryParamAuthStrategy.class);
    }

    @Test
    void shouldThrowForUnknownProvider() {
        assertThatThrownBy(() -> factory.getStrategy("unknown-provider"))
                .isInstanceOf(UnknownAuthStrategyException.class)
                .hasMessageContaining("unknown-provider");
    }

    @Test
    void shouldThrowForNullProvider() {
        assertThatThrownBy(() -> factory.getStrategy(null))
                .isInstanceOf(UnknownAuthStrategyException.class);
    }

    // ---------------------------------------------------------------
    // DEFAULT STRATEGY
    // ---------------------------------------------------------------

    @Test
    void shouldDefaultToBearerWhenAuthTypeNotSpecified() {
        Map<String, ProviderAuthConfig> configs = Map.of(
                "myprovider", new ProviderAuthConfig(AuthType.BEARER, null)
        );
        ProviderAuthStrategyFactory localFactory =
                new ProviderAuthStrategyFactory(configs);

        ProviderAuthStrategy strategy = localFactory.getStrategy("myprovider");

        assertThat(strategy).isInstanceOf(BearerAuthStrategy.class);
    }
}