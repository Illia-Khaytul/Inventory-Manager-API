package io.github.khaytul_illia.inventory_manager_api.common.response.pagination;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(
    description = "Response containing page data and a list of loaded elements",
    example = """
        {
            "page": 0,
            "pageSize": 10,
            "totalPages": 1,
            "totalElements": 3,
            "content": [
                {
                    "name": "example element 1",
                    "otherData": "other"
                },
                {
                    "name": "example element 2",
                    "otherData": "other"
                },
                {
                    "name": "example element 3",
                    "otherData": "other"
                }
            ]
        }
        """
)
public record PaginatedResponse<T>(
    int page,
    int pageSize,
    int totalPages,
    long totalElements,
    List<T> content
) {
}
