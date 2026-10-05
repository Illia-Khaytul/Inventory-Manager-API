package io.github.khaytul_illia.inventory_manager_api.common.pagination;

import java.util.List;

public record PaginatedResponse<T>(
    int page,
    int pageSize,
    int totalPages,
    long totalElements,
    List<T> elements
) {
}
