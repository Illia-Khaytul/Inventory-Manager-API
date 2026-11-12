package io.github.khaytul_illia.inventory_manager_api.product;

import io.github.khaytul_illia.inventory_manager_api.product.request.ProductFiltering;
import io.github.khaytul_illia.inventory_manager_api.security.SecurityUtils;
import io.github.khaytul_illia.inventory_manager_api.user.User;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
public class ProductSpecificationBuilder {

    private final SecurityUtils securityUtils;

    public ProductSpecificationBuilder(SecurityUtils securityUtils) {
        this.securityUtils = securityUtils;
    }

    public Specification<Product> buildSpecificationFromFiltering(ProductFiltering filtering) {
        if (filtering.isEmpty()) {
            return showDeleted(false);
        }

        List<Specification<Product>> filters = new ArrayList<>();
        filters.add(nameContains(filtering.nameContains()));
        filters.add(withMinStock(filtering.minStock()));
        filters.add(withMinPrice(filtering.minPrice()));
        filters.add(withMaxPrice(filtering.maxPrice()));
        filters.add(showDeleted(filtering.showDeleted()));

        return Specification.allOf(filters.stream().filter(Objects::nonNull).toList());
    }

    public ProductFiltering scopeFiltersByUserPermission(ProductFiltering filtering){
        if(securityUtils.isAuthenticatedUserOperator()){
            return filtering;
        }

        return new ProductFiltering(
            filtering.nameContains(),
            filtering.minStock(),
            filtering.minPrice(),
            filtering.maxPrice(),
            false
        );
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

    public Specification<Product> showDeleted(boolean showDeleted){
        if (showDeleted){
            return null;
        }

        return (root, cq, cb) -> cb.isNull(root.get("deletedAt"));
    }

}
