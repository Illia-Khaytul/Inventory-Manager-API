package io.github.khaytul_illia.inventory_manager_api.product;

import io.github.khaytul_illia.inventory_manager_api.common.pagination.PaginatedResponse;
import io.github.khaytul_illia.inventory_manager_api.product.request.CreateProductRequest;
import io.github.khaytul_illia.inventory_manager_api.product.request.ProductFiltering;
import io.github.khaytul_illia.inventory_manager_api.product.request.UpdateProductRequest;
import io.github.khaytul_illia.inventory_manager_api.product.request.ModifyStockRequest;
import io.github.khaytul_illia.inventory_manager_api.product.response.ProductResponse;
import io.github.khaytul_illia.inventory_manager_api.product.response.ProductShortResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = "/products")
@Tag(
    name = "Products",
    description = "API for product management and viewing"
)
@SecurityRequirement(name = "JWT authentication")
public class ProductController {

    @PostMapping(path = "")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<ProductResponse> createProduct(
        @RequestBody @Valid CreateProductRequest request
    ){
        return null;
    }

    @PatchMapping(path = "/{productId}")
    @ResponseStatus(HttpStatus.OK)
    public ProductResponse updateProduct(
        @PathVariable @Valid @Positive long productId,
        @RequestBody @Valid UpdateProductRequest request
    ){
        return null;
    }

    @PatchMapping(path = "/{productId}/stock")
    @ResponseStatus(HttpStatus.OK)
    public ProductResponse changeProductStock(
        @PathVariable @Valid @Positive long productId,
        @RequestBody @Valid ModifyStockRequest request
    ){
        return null;
    }

    @GetMapping(path = "/{productId}")
    @ResponseStatus(HttpStatus.OK)
    public ProductResponse getProduct(
        @PathVariable @Valid @Positive long productId
    ){
        return null;
    }

    @GetMapping(path = "")
    @ResponseStatus(HttpStatus.OK)
    public PaginatedResponse<ProductShortResponse> getProducts(
        @PageableDefault(page = 0, size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pagination,
        @ModelAttribute @Valid ProductFiltering filtering
    ){
        return null;
    }

    @PostMapping(path = "/{productId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProduct(
        @PathVariable @Valid @Positive long productId
    ){

    }

}
