package io.github.khaytul_illia.inventory_manager_api.openapi;

import io.github.khaytul_illia.inventory_manager_api.error.ErrorResponse;
import io.github.khaytul_illia.inventory_manager_api.product.response.ProductResponse;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static io.github.khaytul_illia.inventory_manager_api.openapi.OpenApiConfig.*;

public class ProductEndpointResponseProvider {

    public static Map<String, ApiResponse> provideProductEndpointResponses(){
        Map<String, ApiResponse> responses = new HashMap<>();
        responses.putAll(provideCreateProductResponses());
        responses.putAll(provideUpdateProductResponses());

        return responses.entrySet().stream().collect(Collectors.toMap(
            entry -> "products_" + entry.getKey(),
            Map.Entry::getValue
        ));
    }
    
    private static Map<String, ApiResponse> provideCreateProductResponses(){
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

    private static Map<String, ApiResponse> provideUpdateProductResponses(){
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

}
