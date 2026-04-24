package com.platform.common.cache;

import java.time.Duration;
import java.util.Optional;

/**
 * Provider-agnostic cache contract.
 * Implementations may be Redis-backed (production) or in-memory (tests).
 *
 * @param <K> key type
 * @param <V> value type
 */
public interface GenericCache<K, V> {

    void put(K key, V value, Duration ttl);

    Optional<V> get(K key);

    boolean contains(K key);

    void evict(K key);
}
