package com.rivin.keypilot_gateway.infrastructure.persistence;


import com.rivin.keypilot_gateway.domain.model.ApiKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

class FileBackedApiKeyRepositoryTest {

    @TempDir
    Path tempDir;

    private FileBackedApiKeyRepository repository;

    @BeforeEach
    void setUp() {
        // Each test gets a fresh repository pointed at a temp file
        Path storageFile = tempDir.resolve("keys.json");
        repository = new FileBackedApiKeyRepository(storageFile.toString());
    }

    // ---------------------------------------------------------------
    // SAVE AND FIND
    // ---------------------------------------------------------------

    @Test
    void shouldSaveKeyAndFindById() {
        ApiKey key = new ApiKey("sk-test-123", "openai");

        repository.save(key);

        Optional<ApiKey> found = repository.findById(key.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(key.getId());
        assertThat(found.get().getProvider()).isEqualTo("openai");
    }

    @Test
    void shouldReturnEmptyOptionalForUnknownId() {
        Optional<ApiKey> found = repository.findById("does-not-exist");
        assertThat(found).isEmpty();
    }

    @Test
    void shouldPersistKeyValueAcrossRepositoryInstances() {
        // This is the critical test — simulates a gateway restart
        ApiKey key = new ApiKey("sk-persist-me", "openai");
        Path storageFile = tempDir.resolve("keys.json");

        // Save with first instance
        FileBackedApiKeyRepository firstInstance =
                new FileBackedApiKeyRepository(storageFile.toString());
        firstInstance.save(key);

        // Load with a brand new instance pointing at the same file
        FileBackedApiKeyRepository secondInstance =
                new FileBackedApiKeyRepository(storageFile.toString());

        Optional<ApiKey> found = secondInstance.findById(key.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getProvider()).isEqualTo("openai");
    }

    @Test
    void shouldPreserveActiveStatusAcrossInstances() {
        Path storageFile = tempDir.resolve("keys.json");
        ApiKey key = new ApiKey("sk-active-test", "openai");

        FileBackedApiKeyRepository first =
                new FileBackedApiKeyRepository(storageFile.toString());

        // save initial key
        first.save(key);

        first.findById(key.getId()).ifPresent(k -> {
            k.deactivate();
            first.save(k);
        });

        // simulate new application instance
        FileBackedApiKeyRepository second =
                new FileBackedApiKeyRepository(storageFile.toString());

        Optional<ApiKey> found = second.findById(key.getId());

        assertThat(found).isPresent();
        assertThat(found.get().isActive()).isFalse();
    }

    // ---------------------------------------------------------------
    // FIND ALL
    // ---------------------------------------------------------------

    @Test
    void shouldFindAllSavedKeys() {
        ApiKey key1 = new ApiKey("sk-1", "openai");
        ApiKey key2 = new ApiKey("sk-2", "anthropic");

        repository.save(key1);
        repository.save(key2);

        List<ApiKey> all = repository.findAll();
        assertThat(all).hasSize(2);
    }

    @Test
    void shouldReturnEmptyListWhenNoKeysSaved() {
        assertThat(repository.findAll()).isEmpty();
    }

    @Test
    void shouldFindAllByProvider() {
        ApiKey openai1   = new ApiKey("sk-1", "openai");
        ApiKey openai2   = new ApiKey("sk-2", "openai");
        ApiKey anthropic = new ApiKey("sk-3", "anthropic");

        repository.save(openai1);
        repository.save(openai2);
        repository.save(anthropic);

        List<ApiKey> openaiKeys = repository.findAllByProvider("openai");

        assertThat(openaiKeys).hasSize(2);
        assertThat(openaiKeys)
                .extracting(ApiKey::getProvider)
                .containsOnly("openai");
    }

    @Test
    void shouldReturnEmptyListForUnknownProvider() {
        repository.save(new ApiKey("sk-1", "openai"));

        List<ApiKey> result = repository.findAllByProvider("anthropic");

        assertThat(result).isEmpty();
    }

    // ---------------------------------------------------------------
    // DELETE
    // ---------------------------------------------------------------

    @Test
    void shouldDeleteKeyById() {
        ApiKey key = new ApiKey("sk-delete-me", "openai");
        repository.save(key);

        repository.delete(key.getId());

        assertThat(repository.findById(key.getId())).isEmpty();
    }

    @Test
    void shouldNotAffectOtherKeysWhenDeleting() {
        ApiKey key1 = new ApiKey("sk-keep", "openai");
        ApiKey key2 = new ApiKey("sk-delete", "openai");

        repository.save(key1);
        repository.save(key2);

        repository.delete(key2.getId());

        assertThat(repository.findAll()).hasSize(1);
        assertThat(repository.findById(key1.getId())).isPresent();
    }

    @Test
    void shouldPersistDeletionAcrossInstances() {
        Path storageFile = tempDir.resolve("keys.json");
        ApiKey key = new ApiKey("sk-gone", "openai");

        FileBackedApiKeyRepository first =
                new FileBackedApiKeyRepository(storageFile.toString());
        first.save(key);
        first.delete(key.getId());

        FileBackedApiKeyRepository second =
                new FileBackedApiKeyRepository(storageFile.toString());

        assertThat(second.findById(key.getId())).isEmpty();
        assertThat(second.findAll()).isEmpty();
    }

    // ---------------------------------------------------------------
    // THREAD SAFETY
    // ---------------------------------------------------------------

    @Test
    void shouldHandleConcurrentSavesWithoutCorruption() throws InterruptedException {
        int threadCount = 20;
        Thread[] threads = new Thread[threadCount];

        for (int i = 0; i < threadCount; i++) {
            final int idx = i;
            threads[i] = new Thread(() -> {
                repository.save(new ApiKey("sk-" + idx, "openai" ));
            });
        }

        for (Thread t : threads) t.start();
        for (Thread t : threads) t.join();

        // All 20 saves must have landed correctly
        assertThat(repository.findAll()).hasSize(threadCount);
    }
}