package io.github.khaytul_illia.inventory_manager_api.product;

import io.github.khaytul_illia.inventory_manager_api.product.request.ProductFiltering;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
public class ProductSpecificationBuilder {

    public Specification<Product> buildSpecificationFromFiltering(ProductFiltering filtering) {
        if (filtering.isEmpty()) {
            return Specification.allOf(List.of());
        }

        List<Specification<Product>> filters = new ArrayList<>();
        filters.add(nameContains(filtering.nameContains()));
        filters.add(withMinStock(filtering.minStock()));
        filters.add(withMinPrice(filtering.minPrice()));
        filters.add(withMaxPrice(filtering.maxPrice()));

        return Specification.allOf(filters.stream().filter(Objects::nonNull).toList());
    }

    public Specification<Product> nameContains(String nameContains) {
        if (nameContains == null || nameContains.isEmpty()) {
            return null;
        }

        return (root, cq, cb) -> cb.like(cb.lower(root.get("name")), "%" + nameContains.toLowerCase() + "%");
    }

    public Specification<Product> withMinStock(Integer minStock) {
        if (minStock == null) {
            return null;
        }

        return (root, cq, cb) -> cb.greaterThanOrEqualTo(root.get("stock"), minStock);
    }

    public Specification<Product> withMinPrice(BigDecimal minPrice) {
        if (minPrice == null) {
            return null;
        }

        return (root, cq, cb) -> cb.greaterThanOrEqualTo(root.get("price"), minPrice);
    }

    public Specification<Product> withMaxPrice(BigDecimal maxPrice) {
        if (maxPrice == null) {
            return null;
        }

        return (root, cq, cb) -> cb.lessThanOrEqualTo(root.get("price"), maxPrice);
    }

}
