package com.platform.common.api;

import java.util.List;
import java.util.Objects;

/**
 * Generic pagination wrapper. Replaces ad-hoc Map responses like
 * {@code Map.of("items", list, "total", n)} with a type-safe form.
 *
 * @param <T> type of the items in the page
 */
public final class PagedResult<T> {

    private final List<T> items;
    private final int page;
    private final int size;
    private final long totalElements;
    private final int totalPages;

    public PagedResult(List<T> items, int page, int size, long totalElements) {
        this.items = Objects.requireNonNull(items, "items");
        if (page < 0) throw new IllegalArgumentException("page must be >= 0");
        if (size <= 0) throw new IllegalArgumentException("size must be > 0");
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = (int) Math.ceil((double) totalElements / size);
    }

    public static <T> PagedResult<T> of(List<T> items, int page, int size, long totalElements) {
        return new PagedResult<>(items, page, size, totalElements);
    }

    public List<T> getItems()       { return items; }
    public int getPage()            { return page; }
    public int getSize()            { return size; }
    public long getTotalElements()  { return totalElements; }
    public int getTotalPages()      { return totalPages; }
    public boolean hasNext()        { return page + 1 < totalPages; }
}
