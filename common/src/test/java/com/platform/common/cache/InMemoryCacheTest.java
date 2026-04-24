package com.platform.common.cache;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryCacheTest {

    @Test
    void put_then_get_returns_value_before_ttl_expires() {
        GenericCache<String, Integer> cache = new InMemoryCache<>();
        cache.put("x", 42, Duration.ofSeconds(5));
        assertThat(cache.get("x")).contains(42);
        assertThat(cache.contains("x")).isTrue();
    }

    @Test
    void get_returns_empty_after_ttl_expires() throws InterruptedException {
        GenericCache<String, String> cache = new InMemoryCache<>();
        cache.put("k", "v", Duration.ofMillis(50));
        Thread.sleep(80);
        assertThat(cache.get("k")).isEmpty();
    }

    @Test
    void evict_removes_entry() {
        GenericCache<String, String> cache = new InMemoryCache<>();
        cache.put("k", "v", Duration.ofSeconds(60));
        cache.evict("k");
        assertThat(cache.contains("k")).isFalse();
    }
}
