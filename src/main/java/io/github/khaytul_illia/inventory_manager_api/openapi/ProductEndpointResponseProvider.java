package io.github.khaytul_illia.inventory_manager_api.openapi;

import io.github.khaytul_illia.inventory_manager_api.common.response.pagination.PaginatedResponse;
import io.github.khaytul_illia.inventory_manager_api.error.ErrorResponse;
import io.github.khaytul_illia.inventory_manager_api.product.response.ProductResponse;
import io.github.khaytul_illia.inventory_manager_api.product.response.ProductShortResponse;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static io.github.khaytul_illia.inventory_manager_api.openapi.OpenApiConfig.*;

public class ProductEndpointResponseProvider {

    public static Map<String, ApiResponse> provideProductEndpointResponses() {
        Map<String, ApiResponse> responses = new HashMap<>();
        responses.putAll(provideCreateProductResponses());
        responses.putAll(provideUpdateProductResponses());
        responses.putAll(provideChangeProductStockResponses());
        responses.putAll(provideGetProductResponses());
        responses.putAll(provideGetProductsResponses());
        responses.putAll(provideDeleteProductResponses());

        return responses.entrySet().stream().collect(Collectors.toMap(
            entry -> "products_" + entry.getKey(),
            Map.Entry::getValue
        ));
    }

    private static Map<String, ApiResponse> provideCreateProductResponses() {
        ProductResponse createProductSuccessResponse = new ProductResponse(
            1L,
            "Original Product",
            "product description if any",
            0,
            new BigDecimal("20.05"),
            fixedTime,
            "operator1",
            null,
            null
        );
        ErrorResponse createProduct400ResponseBlankCase = formatErrorResponse(
            new ErrorResponse(HttpStatus.BAD_REQUEST, "Invalid request parameters", Map.of(
                "name", "cannot be blank",
                "description", "must be null or have at least one non-whitespace character",
                "price", "cannot be null"
            ))
        );
        ErrorResponse createProduct400ResponseSizeCase = formatErrorResponse(
            new ErrorResponse(HttpStatus.BAD_REQUEST, "Invalid request parameters", Map.of(
                "name", "size must be between 0 and 100",
                "description", "size must be between 0 and 1000",
                "price", "must be less than or equal to " + Integer.MAX_VALUE
            ))
        );
        ErrorResponse createProduct401Response = formatErrorResponse(
            new ErrorResponse(HttpStatus.UNAUTHORIZED, "Authentication required to access this resource")
        );
        ErrorResponse createProduct403Response = formatErrorResponse(
            new ErrorResponse(HttpStatus.FORBIDDEN, "Forbidden from accessing this resource")
        );
        ErrorResponse createProduct409Response = formatErrorResponse(
            new ErrorResponse(HttpStatus.CONFLICT, "Product with name 'Original Product (copy)' already exists")
        );

        return Map.of(
            "create_product_success",
            buildApiResponse(
                "ProductResponse",
                "Successfully created new product",
                new Example().value(createProductSuccessResponse),
                Map.of(
                    "Location",
                    new Header()
                        .description("Where this entity can be viewed")
                        .schema(new StringSchema().example("http://localhost:8080/products/" + createProductSuccessResponse.id()))
                )
            ),
            "create_product_400",
            buildApiResponse(
                "ErrorResponse",
                "Invalid create product request parameters",
                Map.of(
                    "Null or empty",
                    new Example().value(createProduct400ResponseBlankCase).description("Create product request had null or blank values"),
                    "Invalid size",
                    new Example().value(createProduct400ResponseSizeCase).description("Create product request has parameters of an invalid size")
                )
            ),
            "create_product_401",
            buildApiResponse(
                "ErrorResponse",
                "Accessing without authentication",
                new Example().value(createProduct401Response)
            ),
            "create_product_403",
            buildApiResponse(
                "ErrorResponse",
                "Accessing with authentication but not an OPERATOR",
                new Example().value(createProduct403Response)
            ),
            "create_product_409",
            buildApiResponse(
                "ErrorResponse",
                "Provided product name is not unique",
                new Example().value(createProduct409Response)
            )
        );
    }

    private static Map<String, ApiResponse> provideUpdateProductResponses() {
        ProductResponse updateProductSuccessResponse = new ProductResponse(
            1L,
            "New Product Name",
            "New more detailed product description",
            0,
            new BigDecimal("20.05"),
            fixedTime,
            "operator1",
            fixedTime.plus(100, ChronoUnit.MINUTES),
            "operator2"
        );
        ErrorResponse updateProduct400ResponseProductId = formatErrorResponse(
            new ErrorResponse(HttpStatus.BAD_REQUEST, "Invalid request parameters", Map.of(
                "productId", "must be greater than or equal to 0"
            ))
        );
        ErrorResponse updateProduct400ResponseBlankCase = formatErrorResponse(
            new ErrorResponse(HttpStatus.BAD_REQUEST, "Invalid request parameters", Map.of(
                "name", "must be null or have at least one non-whitespace character",
                "description", "must be null or have at least one non-whitespace character"
            ))
        );
        ErrorResponse updateProduct400ResponseSizeCase1 = formatErrorResponse(
            new ErrorResponse(HttpStatus.BAD_REQUEST, "Invalid request parameters", Map.of(
                "name", "size must be between 0 and 100",
                "description", "size must be between 0 and 1000",
                "price", "must be greater than or equal to 0"
            ))
        );
        ErrorResponse updateProduct400ResponseSizeCase2 = formatErrorResponse(
            new ErrorResponse(HttpStatus.BAD_REQUEST, "Invalid request parameters", Map.of(
                "name", "size must be between 0 and 100",
                "description", "size must be between 0 and 1000",
                "price", "must be less than or equal to " + Integer.MAX_VALUE
            ))
        );
        ErrorResponse updateProduct401Response = formatErrorResponse(
            new ErrorResponse(HttpStatus.UNAUTHORIZED, "Authentication required to access this resource")
        );
        ErrorResponse updateProduct403Response = formatErrorResponse(
            new ErrorResponse(HttpStatus.FORBIDDEN, "Forbidden from accessing this resource")
        );
        ErrorResponse updateProduct404Response = formatErrorResponse(
            new ErrorResponse(HttpStatus.NOT_FOUND, "Product with id 1 does not exist")
        );
        ErrorResponse updateProduct409ResponseTakenNameCase = formatErrorResponse(
            new ErrorResponse(HttpStatus.CONFLICT, "Product with name 'Original Product (copy)' already exists")
        );
        ErrorResponse updateProduct409ResponseConcurrentCase = formatErrorResponse(
            new ErrorResponse(HttpStatus.CONFLICT, "Concurrent modification error")
        );

        return Map.of(
            "update_product_success",
            buildApiResponse(
                "ProductResponse",
                "Successfully updated product",
                new Example().value(updateProductSuccessResponse)
            ),
            "update_product_400",
            buildApiResponse(
                "ErrorResponse",
                "Invalid update product request parameters",
                Map.of(
                    "Invalid productId",
                    new Example().value(updateProduct400ResponseProductId).description("target product id is negative"),
                    "Null or empty",
                    new Example().value(updateProduct400ResponseBlankCase).description("update product request had null or blank values"),
                    "Invalid size 1",
                    new Example().value(updateProduct400ResponseSizeCase1).description("update product request has parameters of an invalid size"),
                    "Invalid size 2",
                    new Example().value(updateProduct400ResponseSizeCase2).description("update product request has parameters of an invalid size")
                )
            ),
            "update_product_401",
            buildApiResponse(
                "ErrorResponse",
                "Accessing without authentication",
                new Example().value(updateProduct401Response)
            ),
            "update_product_403",
            buildApiResponse(
                "ErrorResponse",
                "Accessing with authentication but not an OPERATOR",
                new Example().value(updateProduct403Response)
            ),
            "update_product_404",
            buildApiResponse(
                "ErrorResponse",
                "Updated product does not exist",
                new Example().value(updateProduct404Response)
            ),
            "update_product_409",
            buildApiResponse(
                "ErrorResponse",
                "Provided new product name is not unique or product was modified concurrently",
                Map.of(
                    "New product name not unique",
                    new Example().value(updateProduct409ResponseTakenNameCase),
                    "Concurrent modification",
                    new Example().value(updateProduct409ResponseConcurrentCase)
                )
            )
        );
    }

    private static Map<String, ApiResponse> provideChangeProductStockResponses() {
        ProductResponse changeProductStockSuccessResponse = new ProductResponse(
            1L,
            "Original Product",
            "product description if any",
            110,
            new BigDecimal("20.05"),
            fixedTime,
            "operator1",
            fixedTime.plus(100, ChronoUnit.MINUTES),
            "operator2"
        );
        ErrorResponse changeProductStock400ResponseProductId = formatErrorResponse(
            new ErrorResponse(HttpStatus.BAD_REQUEST, "Invalid request parameters", Map.of(
                "productId", "must be greater than or equal to 0"
            ))
        );
        ErrorResponse changeProductStock400ResponseNullCase = formatErrorResponse(
            new ErrorResponse(HttpStatus.BAD_REQUEST, "Invalid request parameters", Map.of(
                "stockChange", "must not be null"
            ))
        );
        ErrorResponse changeProductStock400ResponseZeroCase = formatErrorResponse(
            new ErrorResponse(HttpStatus.BAD_REQUEST, "Invalid request parameters", Map.of(
                "stockChange", "must not be 0"
            ))
        );
        ErrorResponse changeProductStock401Response = formatErrorResponse(
            new ErrorResponse(HttpStatus.UNAUTHORIZED, "Authentication required to access this resource")
        );
        ErrorResponse changeProductStock403Response = formatErrorResponse(
            new ErrorResponse(HttpStatus.FORBIDDEN, "Forbidden from accessing this resource")
        );
        ErrorResponse changeProductStock404Response = formatErrorResponse(
            new ErrorResponse(HttpStatus.NOT_FOUND, "Product with id 1 does not exist")
        );
        ErrorResponse changeProductStock409Response = formatErrorResponse(
            new ErrorResponse(HttpStatus.CONFLICT, "Invalid stock modification", Map.of(
                "details", List.of("Tried modifying stock by -10 for 5 existing")
            ))
        );

        return Map.of(
            "change_product_stock_success",
            buildApiResponse(
                "ProductResponse",
                "Successfully changed product stock",
                new Example().value(changeProductStockSuccessResponse)
            ),
            "change_product_stock_400",
            buildApiResponse(
                "ErrorResponse",
                "Invalid modify stock request parameters",
                Map.of(
                    "Invalid productId",
                    new Example().value(changeProductStock400ResponseProductId).description("target product id is negative"),
                    "Null value",
                    new Example().value(changeProductStock400ResponseNullCase).description("change product stock request had a null value"),
                    "Zero value",
                    new Example().value(changeProductStock400ResponseZeroCase).description("change product stock request has a value of 0")
                )
            ),
            "change_product_stock_401",
            buildApiResponse(
                "ErrorResponse",
                "Accessing without authentication",
                new Example().value(changeProductStock401Response)
            ),
            "change_product_stock_403",
            buildApiResponse(
                "ErrorResponse",
                "Accessing with authentication but not an OPERATOR",
                new Example().value(changeProductStock403Response)
            ),
            "change_product_stock_404",
            buildApiResponse(
                "ErrorResponse",
                "Target product does not exist",
                new Example().value(changeProductStock404Response)
            ),
            "change_product_stock_409",
            buildApiResponse(
                "ErrorResponse",
                "Product stock modification is invalid",
                new Example().value(changeProductStock409Response)
            )
        );
    }

    private static Map<String, ApiResponse> provideGetProductResponses() {
        ProductResponse getProductSuccessResponseOperator = new ProductResponse(
            1L,
            "Original Product",
            "product description if any",
            110,
            new BigDecimal("20.05"),
            fixedTime,
            "operator1",
            fixedTime.plus(100, ChronoUnit.MINUTES),
            "operator2"
        );
        ProductResponse getProductSuccessResponseCustomer = new ProductResponse(
            1L,
            "Original Product",
            "product description if any",
            110,
            new BigDecimal("20.05"),
            null,
            null,
            null,
            null
        );
        ErrorResponse getProduct400Response = formatErrorResponse(
            new ErrorResponse(HttpStatus.BAD_REQUEST, "Invalid request parameters", Map.of(
                "productId", "must be greater than or equal to 0"
            ))
        );
        ErrorResponse getProduct404Response = formatErrorResponse(
            new ErrorResponse(HttpStatus.NOT_FOUND, "Product with id 1 does not exist")
        );

        return Map.of(
            "get_product_success",
            buildApiResponse(
                "ProductResponse",
                "Successfully loaded product",
                Map.of(
                    "OPERATOR response",
                    new Example().value(getProductSuccessResponseOperator).description("Full response with audit fields for OPERATOR users"),
                    "Non OPERATOR response",
                    new Example().value(getProductSuccessResponseCustomer).description("Short response without audit fields for non OPERATOR users")
                )
            ),
            "get_product_400",
            buildApiResponse(
                "ErrorResponse",
                "Invalid productId",
                new Example().value(getProduct400Response)
            ),
            "get_product_404",
            buildApiResponse(
                "ErrorResponse",
                "Target product does not exist",
                new Example().value(getProduct404Response)
            )
        );
    }

    private static Map<String, ApiResponse> provideGetProductsResponses() {
        PaginatedResponse<ProductShortResponse> getProductsSuccessResponse = new PaginatedResponse<>(
            0,
            10,
            1,
            3,
            List.of(
                new ProductShortResponse(1L, "Original Product v1", 10, new BigDecimal("100.99")),
                new ProductShortResponse(2L, "Original Product v2", 59, new BigDecimal("159.99")),
                new ProductShortResponse(5L, "Different Product", 15, new BigDecimal("60.5"))
            )
        );
        ErrorResponse getProducts400ResponseSizeCase1 = formatErrorResponse(
            new ErrorResponse(HttpStatus.BAD_REQUEST, "Invalid request parameters", Map.of(
                "minStock", "must be greater than or equal to 0",
                "minPrice", "must be greater than or equal to 0",
                "maxPrice", "must be greater than or equal to 0"
            ))
        );
        ErrorResponse getProducts400ResponseSizeCase2 = formatErrorResponse(
            new ErrorResponse(HttpStatus.BAD_REQUEST, "Invalid request parameters", Map.of(
                "nameContains", "size must be between 0 and 100",
                "minPrice", "must be less than or equal to " + Integer.MAX_VALUE,
                "maxPrice", "must be less than or equal to " + Integer.MAX_VALUE
            ))
        );

        return Map.of(
            "get_products_success",
            buildApiResponse(
                "ProductResponse",
                "Successfully loaded product page",
                new Example().value(getProductsSuccessResponse)
            ),
            "get_products_400",
            buildApiResponse(
                "ErrorResponse",
                "Invalid product filtering parameters",
                Map.of(
                    "Invalid size 1",
                    new Example().value(getProducts400ResponseSizeCase1).description("product filtering has parameters of an invalid size"),
                    "Invalid size 2",
                    new Example().value(getProducts400ResponseSizeCase2).description("product filtering has parameters of an invalid size")
                )
            )
        );
    }

    private static Map<String, ApiResponse> provideDeleteProductResponses(){
        ErrorResponse deleteProduct400Response = formatErrorResponse(
            new ErrorResponse(HttpStatus.BAD_REQUEST, "Invalid request parameters", Map.of(
                "productId", "must be greater than or equal to 0"
            ))
        );
        ErrorResponse deleteProduct401Response = formatErrorResponse(
            new ErrorResponse(HttpStatus.UNAUTHORIZED, "Authentication required to access this resource")
        );
        ErrorResponse deleteProduct403Response = formatErrorResponse(
            new ErrorResponse(HttpStatus.FORBIDDEN, "Forbidden from accessing this resource")
        );
        
        return Map.of(
            "delete_product_success",
            new ApiResponse().description("Successfully deleted product"),
            "delete_product_400",
            buildApiResponse(
                "ErrorResponse",
                "Invalid productId",
                new Example().value(deleteProduct400Response)
            ),
            "delete_product_401",
            buildApiResponse(
                "ErrorResponse",
                "Accessing without authentication",
                new Example().value(deleteProduct401Response)
            ),
            "delete_product_403",
            buildApiResponse(
                "ErrorResponse",
                "Accessing with authentication but not an OPERATOR",
                new Example().value(deleteProduct403Response)
            )
        );
    }

}
