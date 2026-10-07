package io.github.khaytul_illia.inventory_manager_api.product;

import io.github.khaytul_illia.inventory_manager_api.common.pagination.PaginatedResponse;
import io.github.khaytul_illia.inventory_manager_api.product.request.CreateProductRequest;
import io.github.khaytul_illia.inventory_manager_api.product.request.ProductFiltering;
import io.github.khaytul_illia.inventory_manager_api.product.request.UpdateProductRequest;
import io.github.khaytul_illia.inventory_manager_api.product.request.ModifyStockRequest;
import io.github.khaytul_illia.inventory_manager_api.product.response.ProductResponse;
import io.github.khaytul_illia.inventory_manager_api.product.response.ProductShortResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping(path = "/products")
@Tag(
    name = "Products",
    description = "API for product management and viewing"
)
@SecurityRequirement(name = "JWT authentication")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping(path = "")
    @Operation(
        summary = "Create new product",
        description = """
            Creates a new product with the provided data and empty stock.
            - Returns with 201 Created on successful product creation.
            - Returns with 400 Bad Request if the create product request is invalid.
            - Returns with 401 Unauthorized if user is not authenticated.
            - Returns with 403 Forbidden if the authenticated user is not an OPERATOR.
            - Returns with 409 Conflict if the provided product name is not unique.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", ref = "#/components/responses/products_create_product_success"),
        @ApiResponse(responseCode = "400", ref = "#/components/responses/products_create_product_400"),
        @ApiResponse(responseCode = "401", ref = "#/components/responses/products_create_product_401"),
        @ApiResponse(responseCode = "403", ref = "#/components/responses/products_create_product_403"),
        @ApiResponse(responseCode = "409", ref = "#/components/responses/products_create_product_409")
    })
    public ResponseEntity<ProductResponse> createProduct(
        @RequestBody @Valid CreateProductRequest request
    ){
        ProductResponse response = productService.createProduct(request);

        return ResponseEntity
            .created(ServletUriComponentsBuilder
                .fromCurrentRequestUri()
                .path("/{productId}")
                .build(response.id())
            )
            .body(response);
    }

    @PatchMapping(path = "/{productId}")
    @ResponseStatus(HttpStatus.OK)
    public ProductResponse updateProduct(
        @PathVariable @Valid @Positive long productId,
        @RequestBody @Valid UpdateProductRequest request
    ){
        return productService.updateProduct(productId, request);
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
