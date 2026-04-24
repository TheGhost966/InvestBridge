package com.platform.common.cache;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe in-memory {@link GenericCache} used in tests and as a fallback
 * when Redis is not available.
 *
 * @param <K> key type
 * @param <V> value type
 */
public class InMemoryCache<K, V> implements GenericCache<K, V> {

    private static final class Entry<V> {
        final V value;
        final Instant expiresAt;

        Entry(V value, Instant expiresAt) {
            this.value = value;
            this.expiresAt = expiresAt;
        }

        boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }

    private final Map<K, Entry<V>> store = new ConcurrentHashMap<>();

    @Override
    public void put(K key, V value, Duration ttl) {
        store.put(key, new Entry<>(value, Instant.now().plus(ttl)));
    }

    @Override
    public Optional<V> get(K key) {
        Entry<V> entry = store.get(key);
        if (entry == null) return Optional.empty();
        if (entry.isExpired()) {
            store.remove(key);
            return Optional.empty();
        }
        return Optional.of(entry.value);
    }

    @Override
    public boolean contains(K key) {
        return get(key).isPresent();
    }

    @Override
    public void evict(K key) {
        store.remove(key);
    }
}
