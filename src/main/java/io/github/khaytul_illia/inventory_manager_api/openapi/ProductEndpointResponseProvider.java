package io.github.khaytul_illia.inventory_manager_api.openapi;

import io.github.khaytul_illia.inventory_manager_api.error.ErrorResponse;
import io.github.khaytul_illia.inventory_manager_api.product.response.ProductResponse;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static io.github.khaytul_illia.inventory_manager_api.openapi.OpenApiConfig.*;

public class ProductEndpointResponseProvider {

    public static Map<String, ApiResponse> provideProductEndpointResponses(){
        Map<String, ApiResponse> responses = new HashMap<>();
        responses.putAll(provideCreateProductResponses());

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
                "price", "cannot be null"
            ))
        );
        ErrorResponse createProduct400ResponseSizeCase = formatErrorResponse(
            new ErrorResponse(HttpStatus.BAD_REQUEST, "Invalid request parameters", Map.of(
                "name", "size must be between 0 and 100",
                "description", "size must be between 0 and 1000",
                "price", "must be between 0 and " + Integer.MAX_VALUE
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
    
}
