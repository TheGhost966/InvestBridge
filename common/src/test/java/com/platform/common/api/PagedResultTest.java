package com.platform.common.api;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PagedResultTest {

    @Test
    void computes_totalPages_correctly() {
        PagedResult<String> p = PagedResult.of(List.of("a", "b"), 0, 2, 5);
        assertThat(p.getTotalPages()).isEqualTo(3);
        assertThat(p.hasNext()).isTrue();
    }

    @Test
    void last_page_hasNext_is_false() {
        PagedResult<String> p = PagedResult.of(List.of("x"), 2, 2, 5);
        assertThat(p.hasNext()).isFalse();
    }

    @Test
    void rejects_invalid_page_or_size() {
        assertThatThrownBy(() -> PagedResult.of(List.of(), -1, 10, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PagedResult.of(List.of(), 0, 0, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
