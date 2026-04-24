package com.platform.common.api;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApiResponseTest {

    @Test
    void ok_wraps_payload_and_leaves_errors_null() {
        ApiResponse<String> r = ApiResponse.ok("hello");
        assertThat(r.getData()).isEqualTo("hello");
        assertThat(r.getErrors()).isNull();
        assertThat(r.getTimestamp()).isNotNull();
    }

    @Test
    void error_carries_messages_and_null_data() {
        ApiResponse<Void> r = ApiResponse.error("bad email", "short password");
        assertThat(r.getData()).isNull();
        assertThat(r.getErrors()).containsExactly("bad email", "short password");
    }

    @Test
    void generic_type_is_preserved_across_nesting() {
        ApiResponse<PagedResult<Integer>> r =
                ApiResponse.ok(PagedResult.of(java.util.List.of(1, 2, 3), 0, 10, 3));

        assertThat(r.getData().getItems()).containsExactly(1, 2, 3);
        assertThat(r.getData().getTotalPages()).isEqualTo(1);
    }
}
