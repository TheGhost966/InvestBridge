package com.platform.desktop.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Mirror of {@code com.platform.common.api.PagedResult&lt;T&gt;} — the generic
 * pagination envelope returned by {@code /ideas/paged}.
 *
 * <p>Generic on {@code T} so a single Jackson deserialiser handles any element
 * type. Uses {@link com.fasterxml.jackson.core.type.TypeReference} at the call
 * site to preserve the parameter through erasure.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PagedResult<T> {

    public List<T> items;
    public int  page;
    public int  size;
    public int  totalPages;
    public long totalElements;

    public PagedResult() {}
}
