package com.migration.common;

import java.util.List;

public record PageResponse<T>(
    List<T> content,
    int page,
    int size,
    long totalElements,
    int totalPages
) {
    public static final int DEFAULT_SIZE = 20;
    public static final int MIN_SIZE = 10;
    public static final int MAX_SIZE = 500;

    public static int clampSize(Integer size) {
        if (size == null) return DEFAULT_SIZE;
        return Math.min(MAX_SIZE, Math.max(MIN_SIZE, size));
    }

    public static int clampPage(Integer page) {
        if (page == null || page < 0) return 0;
        return page;
    }

    public static <T> PageResponse<T> of(List<T> content, int page, int size, long totalElements) {
        int totalPages = size <= 0 ? 0 : (int) Math.ceil((double) totalElements / size);
        return new PageResponse<>(content, page, size, totalElements, totalPages);
    }
}
