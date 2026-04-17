package com.rivin.keypilot_gateway.infrastructure.proxy.auth;


public enum AuthType {
    BEARER,        // Authorization: Bearer <key>   — OpenAI, Anthropic
    QUERY_PARAM,   // ?key=<key>                    — Gemini
    API_KEY_HEADER // x-api-key: <key>              — Cohere, others
}
