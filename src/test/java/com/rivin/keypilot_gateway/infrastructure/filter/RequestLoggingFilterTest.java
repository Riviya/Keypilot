package com.rivin.keypilot_gateway.infrastructure.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RequestLoggingFilterTest {

    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private FilterChain filterChain;

    private RequestLoggingFilter filter;

    @BeforeEach
    void setUp() {
        filter = new RequestLoggingFilter();
    }

    @AfterEach
    void tearDown() {
        // Always clean up MDC between tests — MDC is thread-local
        MDC.clear();
    }

    // ---------------------------------------------------------------
    // CORRELATION ID
    // ---------------------------------------------------------------

    @Test
    void shouldSetCorrelationIdInMdcBeforeChainExecutes() throws Exception {
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/v1/chat/completions");
        when(request.getHeader("X-Correlation-ID")).thenReturn(null);

        // Capture MDC state during filter chain execution
        String[] capturedId = new String[1];
        doAnswer(invocation -> {
            capturedId[0] = MDC.get("correlationId");
            return null;
        }).when(filterChain).doFilter(request, response);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(capturedId[0]).isNotNull();
        assertThat(capturedId[0]).isNotBlank();
    }

    @Test
    void shouldUseIncomingCorrelationIdIfPresent() throws Exception {
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/v1/models");
        when(request.getHeader("X-Correlation-ID")).thenReturn("caller-provided-id");

        String[] capturedId = new String[1];
        doAnswer(invocation -> {
            capturedId[0] = MDC.get("correlationId");
            return null;
        }).when(filterChain).doFilter(request, response);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(capturedId[0]).isEqualTo("caller-provided-id");
    }

    @Test
    void shouldClearMdcAfterRequestCompletes() throws Exception {
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/v1/chat/completions");
        when(request.getHeader("X-Correlation-ID")).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        // MDC must be clean after the request — no leakage to next request
        assertThat(MDC.get("correlationId")).isNull();
    }

    @Test
    void shouldClearMdcEvenWhenFilterChainThrows() throws Exception {
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/v1/chat/completions");
        when(request.getHeader("X-Correlation-ID")).thenReturn(null);
        doThrow(new RuntimeException("chain exploded"))
                .when(filterChain).doFilter(request, response);

        assertThatThrownBy(() ->
                filter.doFilterInternal(request, response, filterChain)
        ).isInstanceOf(RuntimeException.class);

        // MDC must be clean even after exception
        assertThat(MDC.get("correlationId")).isNull();
    }

    @Test
    void shouldSetCorrelationIdResponseHeader() throws Exception {
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/v1/models");
        when(request.getHeader("X-Correlation-ID")).thenReturn("test-id-123");

        filter.doFilterInternal(request, response, filterChain);

        // Response must echo back the correlation ID
        verify(response).setHeader("X-Correlation-ID", "test-id-123");
    }

    // ---------------------------------------------------------------
    // SECURITY — API KEYS MUST NEVER APPEAR IN LOGS
    // This is a structural test: verify the filter never reads
    // the Authorization header value (which could contain a key)
    // ---------------------------------------------------------------

    @Test
    void shouldNeverReadAuthorizationHeaderValue() throws Exception {
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/v1/chat/completions");
        when(request.getHeader("X-Correlation-ID")).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        // Filter must never touch the Authorization header
        verify(request, never()).getHeader("Authorization");
    }
}
