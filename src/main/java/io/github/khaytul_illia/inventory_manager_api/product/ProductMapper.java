package io.github.khaytul_illia.inventory_manager_api.product;

import io.github.khaytul_illia.inventory_manager_api.common.response.pagination.PaginatedResponse;
import io.github.khaytul_illia.inventory_manager_api.product.request.CreateProductRequest;
import io.github.khaytul_illia.inventory_manager_api.product.request.UpdateProductRequest;
import io.github.khaytul_illia.inventory_manager_api.product.response.ProductResponse;
import io.github.khaytul_illia.inventory_manager_api.product.response.ProductShortResponse;
import org.mapstruct.*;
import org.springframework.data.domain.Page;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ProductMapper {

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "name", source = "name")
    @Mapping(target = "description", source = "description")
    @Mapping(target = "stock", constant = "0")
    @Mapping(target = "price", source = "price")
    Product buildProduct(CreateProductRequest request);

    @BeanMapping(
        ignoreByDefault = true,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
    )
    @Mapping(target = "name", source = "name")
    @Mapping(target = "description", source = "description")
    @Mapping(target = "price", source = "price")
    void updateProduct(@MappingTarget Product product, UpdateProductRequest request);

    ProductResponse toResponse(Product product);

    ProductShortResponse toShortResponse(Product product);

    @Mapping(target = "page", source = "number")
    @Mapping(target = "pageSize", source = "size")
    @Mapping(target = "content", defaultExpression = "java(List.of())")
    PaginatedResponse<ProductShortResponse> toPaginatedResponse(Page<Product> products);

}
