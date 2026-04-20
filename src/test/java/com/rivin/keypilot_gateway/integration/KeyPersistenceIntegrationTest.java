package com.rivin.keypilot_gateway.integration;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class KeyPersistenceIntegrationTest {

    @TempDir
    static Path tempDir;

    @DynamicPropertySource
    static void overrideStoragePath(DynamicPropertyRegistry registry) {
        registry.add("gateway.storage.path",
                () -> tempDir.resolve("persistence-test-keys.json").toString());
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    // ---------------------------------------------------------------
    // KEY CRUD VIA REST — end to end through controller → service → file
    // ---------------------------------------------------------------

    @Test
    void shouldPersistKeyAddedViaRestEndpoint() throws Exception {
        // Add a key via the REST API (what the CLI calls)
        MvcResult addResult = mockMvc.perform(post("/api/keys")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {"provider":"openai","keyValue":"sk-persist-test"}
                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.provider").value("openai"))
                .andReturn();

        String responseBody = addResult.getResponse().getContentAsString();
        String keyId = objectMapper.readTree(responseBody).get("id").asText();

        // List keys — must include the one we just added
        mockMvc.perform(get("/api/keys"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == '" + keyId + "')]").exists());
    }

    @Test
    void shouldDeleteKeyAndNotReturnItInList() throws Exception {
        // Add
        MvcResult addResult = mockMvc.perform(post("/api/keys")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {"keyValue":"sk-to-delete", "provider":"openai"}
                """))
                .andExpect(status().isCreated())
                .andReturn();

        String keyId = objectMapper.readTree(
                addResult.getResponse().getContentAsString()
        ).get("id").asText();

        // Delete
        mockMvc.perform(delete("/api/keys/" + keyId))
                .andExpect(status().isNoContent());

        // List — must not contain deleted key
        mockMvc.perform(get("/api/keys"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == '" + keyId + "')]").doesNotExist());
    }

    @Test
    void shouldReturn404WhenDeletingNonExistentKey() throws Exception {
        mockMvc.perform(delete("/api/keys/ghost-id-that-does-not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void shouldReturn400WhenAddingKeyWithMissingProvider() throws Exception {
        mockMvc.perform(post("/api/keys")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {"keyValue":"sk-no-provider"}
                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn400WhenAddingKeyWithMissingKeyValue() throws Exception {
        mockMvc.perform(post("/api/keys")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {"provider":"openai"}
                """))
                .andExpect(status().isBadRequest());
    }
}
