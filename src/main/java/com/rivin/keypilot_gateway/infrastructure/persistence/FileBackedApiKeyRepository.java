package com.rivin.keypilot_gateway.infrastructure.persistence;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rivin.keypilot_gateway.application.port.ApiKeyRepository;
import com.rivin.keypilot_gateway.domain.model.ApiKey;
import com.rivin.keypilot_gateway.infrastructure.Exception.StorageException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

public class FileBackedApiKeyRepository implements ApiKeyRepository {

    private final Path storagePath;
    private final ObjectMapper objectMapper;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public FileBackedApiKeyRepository(String storagePath) {
        this.storagePath = Paths.get(storagePath);
        this.objectMapper = new ObjectMapper();
        initializeStorageFile();
    }

    // ---------------------------------------------------------------
    // PUBLIC API (implements ApiKeyRepository)
    // ---------------------------------------------------------------

    @Override
    public void save(ApiKey apiKey) {
        lock.writeLock().lock();
        try {
            Map<String, ApiKeyRecord> store = readFromDisk();
            store.put(apiKey.getId(), toRecord(apiKey));
            writeToDisk(store);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public Optional<ApiKey> findById(String id) {
        lock.readLock().lock();
        try {
            Map<String, ApiKeyRecord> store = readFromDisk();
            return Optional.ofNullable(store.get(id))
                    .map(this::toDomain);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public List<ApiKey> findAllByProvider(String provider) {
        lock.readLock().lock();
        try {
            return readFromDisk().values().stream()
                    .filter(r -> r.provider().equalsIgnoreCase(provider))
                    .map(this::toDomain)
                    .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public List<ApiKey> findAll() {
        lock.readLock().lock();
        try {
            return readFromDisk().values().stream()
                    .map(this::toDomain)
                    .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void delete(String id) {
        lock.writeLock().lock();
        try {
            Map<String, ApiKeyRecord> store = readFromDisk();
            store.remove(id);
            writeToDisk(store);
        } finally {
            lock.writeLock().unlock();
        }
    }

    // ---------------------------------------------------------------
    // PRIVATE — file I/O
    // ---------------------------------------------------------------

    private void initializeStorageFile() {
        try {
            if (!Files.exists(storagePath)) {
                Files.createDirectories(storagePath.getParent());
                Files.createFile(storagePath);
                writeToDisk(new HashMap<>());
            }
        } catch (IOException e) {
            throw new StorageException("Failed to initialize key storage at: "
                    + storagePath, e);
        }
    }

    private Map<String, ApiKeyRecord> readFromDisk() {
        try {
            byte[] content = Files.readAllBytes(storagePath);
            if (content.length == 0) return new HashMap<>();

            return objectMapper.readValue(
                    content,
                    new TypeReference<Map<String, ApiKeyRecord>>() {}
            );
        } catch (IOException e) {
            throw new StorageException("Failed to read key storage from: "
                    + storagePath, e);
        }
    }

    private void writeToDisk(Map<String, ApiKeyRecord> store) {
        try {
            objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValue(storagePath.toFile(), store);
        } catch (IOException e) {
            throw new StorageException("Failed to write key storage to: "
                    + storagePath, e);
        }
    }

    // ---------------------------------------------------------------
    // PRIVATE — mapping
    // ---------------------------------------------------------------

    private ApiKeyRecord toRecord(ApiKey key) {
        return new ApiKeyRecord(
                key.getId(),
                key.getProvider(),
                key.getKeyValue(),
                key.isActive(),
                key.getMaxRequestsPerWindow(),
                key.getWindowDurationSeconds()
        );
    }

    private ApiKey toDomain(ApiKeyRecord record) {
        ApiKey key = new ApiKey(
                record.id(),
                record.provider(),
                record.keyValue(),
                record.active(),
                record.maxRequestsPerWindow(),
                record.windowDurationSeconds()
        );
        return key;
    }
}