package io.github.khaytul_illia.inventory_manager_api.product;

import io.github.khaytul_illia.inventory_manager_api.common.response.pagination.PaginatedResponse;
import io.github.khaytul_illia.inventory_manager_api.support.context.ControllerTestContext;
import io.github.khaytul_illia.inventory_manager_api.product.request.CreateProductRequest;
import io.github.khaytul_illia.inventory_manager_api.product.request.ModifyStockRequest;
import io.github.khaytul_illia.inventory_manager_api.product.request.ProductFiltering;
import io.github.khaytul_illia.inventory_manager_api.product.request.UpdateProductRequest;
import io.github.khaytul_illia.inventory_manager_api.product.response.ProductResponse;
import io.github.khaytul_illia.inventory_manager_api.product.response.ProductShortResponse;
import io.github.khaytul_illia.inventory_manager_api.user.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.util.MultiValueMap;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static io.github.khaytul_illia.inventory_manager_api.support.utils.mvc.ErrorResponseAssertionUtils.*;
import static io.github.khaytul_illia.inventory_manager_api.support.utils.mvc.SuccessResponseAssertionUtils.*;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("ProductController tests")
public class ProductControllerTests extends ControllerTestContext {

    @Nested
    @DisplayName("createProduct endpoint tests")
    class CreateProductTests {

        private final String price = "99.99";
        private final CreateProductRequest request = new CreateProductRequest("product_name", null, new BigDecimal(price));

        @Test
        @WithMockUser(roles = "OPERATOR")
        @DisplayName("Should return 201 Created when successfully created new product")
        void shouldReturn201_whenSuccessfullyCreatedProduct() throws Exception {
            //Arrange
            Instant createdAt = Instant.now();
            String createdBy = "operator";
            ProductResponse response = new ProductResponse(1L, "product_name", "description", 0, new BigDecimal(price), createdAt, createdBy, createdAt, createdBy);

            when(productService.createProduct(any(CreateProductRequest.class)))
                .thenReturn(response);

            //Act and Assert
            ResultActions result = mockMvc.perform(
                    post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", String.format("http://localhost/products/%s", response.id())));

            assertProductResponse(result, response);

            verify(productService).createProduct(any(CreateProductRequest.class));
        }

        @Test
        @WithMockUser(roles = "CUSTOMER")
        @DisplayName("Should return 403 Forbidden when accessed with authentication but wrong role")
        void shouldReturn403_whenAuthenticatedWithWrongRole() throws Exception {
            //Act and Assert
            ResultActions result = mockMvc.perform(
                    post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isForbidden());

            assertForbiddenErrorResponse(result);
        }

        @Test
        @DisplayName("Should return 401 Unauthorized when accessed with no authentication")
        void shouldReturn401_whenNoAuthentication() throws Exception {
            //Act and Assert
            ResultActions result = mockMvc.perform(
                    post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isUnauthorized());

            assertNoAuthErrorResponse(result);
        }

        @ParameterizedTest
        @MethodSource("provideInvalidCreateProductRequests")
        @WithMockUser(roles = "OPERATOR")
        @DisplayName("Should return 400 Bad Request when invalid create product request fields")
        void shouldReturn400_whenInvalidCreateProductRequest(
            String name,
            String description,
            BigDecimal price,
            String nameMessage,
            String descriptionMessage,
            String priceMessage
        ) throws Exception {
            //Arrange
            CreateProductRequest request = new CreateProductRequest(name, description, price);

            //Act and Assert
            ResultActions result = mockMvc.perform(
                    post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isBadRequest());

            assertSingleErrorValidationErrorResponse(result, Map.of(
                "name", nameMessage,
                "description", descriptionMessage,
                "price", priceMessage
            ));
        }

        /*
                Test data provider methods
         */

        static Stream<Arguments> provideInvalidCreateProductRequests() {
            String longDescription = "r".repeat(1001);
            String longName = "r".repeat(101);

            return Stream.of(
                Arguments.of(
                    null, "", null,
                    "must not be blank", "must be null or have at least one non-whitespace character", "must not be null"
                ),
                Arguments.of(
                    "", "   ", new BigDecimal(-1),
                    "must not be blank", "must be null or have at least one non-whitespace character", "must be between 0.00 and " + Integer.MAX_VALUE + ".00"
                ),
                Arguments.of(
                    "   ", longDescription, new BigDecimal(Integer.MAX_VALUE + ".01"),
                    "must not be blank", "size must be between 0 and 1000", "must be between 0.00 and " + Integer.MAX_VALUE + ".00"
                ),
                Arguments.of(
                    longName, longDescription, null,
                    "size must be between 0 and 100", "size must be between 0 and 1000", "must not be null"
                )
            );
        }

    }

    @Nested
    @DisplayName("updateProduct endpoint tests")
    class UpdateProductTests {

        private final long productId = 1L;
        private final String price = "99.99";
        private final UpdateProductRequest request = new UpdateProductRequest("new_product_name", null, new BigDecimal(price));

        @Test
        @WithMockUser(roles = "OPERATOR")
        @DisplayName("Should return 200 OK when successfully updated a product")
        void shouldReturn200_whenSuccessfullyUpdatedProduct() throws Exception {
            //Arrange
            Instant createdAt = Instant.now();
            String createdBy = "operator";
            ProductResponse response = new ProductResponse(1L, "new_product_name", "description", 0, new BigDecimal(price), createdAt, createdBy, createdAt, createdBy);

            when(productService.updateProduct(anyLong(), any(UpdateProductRequest.class)))
                .thenReturn(response);

            //Act and Assert
            ResultActions result = mockMvc.perform(
                    patch("/products/{productId}", productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isOk());

            assertProductResponse(result, response);

            verify(productService).updateProduct(anyLong(), any(UpdateProductRequest.class));
        }

        @Test
        @WithMockUser(roles = "CUSTOMER")
        @DisplayName("Should return 403 Forbidden when accessed with authentication but wrong role")
        void shouldReturn403_whenAuthenticatedWithWrongRole() throws Exception {
            //Act and Assert
            ResultActions result = mockMvc.perform(
                    patch("/products/{productId}", productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isForbidden());

            assertForbiddenErrorResponse(result);
        }

        @Test
        @DisplayName("Should return 401 Unauthorized when accessed with no authentication")
        void shouldReturn401_whenNoAuthentication() throws Exception {
            //Act and Assert
            ResultActions result = mockMvc.perform(
                    patch("/products/{productId}", productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isUnauthorized());

            assertNoAuthErrorResponse(result);
        }

        @ParameterizedTest
        @MethodSource("provideInvalidUpdateProductRequests")
        @WithMockUser(roles = "OPERATOR")
        @DisplayName("Should return 400 Bad Request when invalid update product request fields")
        void shouldReturn400_whenInvalidCreateProductRequest(
            long productId,
            String name,
            String description,
            BigDecimal price,
            String productIdMessage,
            String nameMessage,
            String descriptionMessage,
            String priceMessage
        ) throws Exception {
            //Arrange
            UpdateProductRequest request = new UpdateProductRequest(name, description, price);

            //Act and Assert
            ResultActions result = mockMvc.perform(
                    patch("/products/{productId}", productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isBadRequest());

            assertSingleErrorValidationErrorResponse(result, Map.of(
                "product_id", productIdMessage,
                "name", nameMessage,
                "description", descriptionMessage,
                "price", priceMessage
            ));
        }

        /*
                Test data provider methods
         */

        static Stream<Arguments> provideInvalidUpdateProductRequests() {
            String longDescription = "r".repeat(1001);
            String longName = "r".repeat(101);

            return Stream.of(
                Arguments.of(
                    -1, "", "", new BigDecimal(-1),
                    "must be greater than 0",
                    "must be null or have at least one non-whitespace character",
                    "must be null or have at least one non-whitespace character",
                    "must be between 0.00 and " + Integer.MAX_VALUE + ".00"
                ),
                Arguments.of(
                    -1, "   ", "   ", new BigDecimal(Integer.MAX_VALUE + ".01"),
                    "must be greater than 0",
                    "must be null or have at least one non-whitespace character",
                    "must be null or have at least one non-whitespace character",
                    "must be between 0.00 and " + Integer.MAX_VALUE + ".00"
                ),
                Arguments.of(
                    -1, longName, longDescription, new BigDecimal(-1),
                    "must be greater than 0",
                    "size must be between 0 and 100",
                    "size must be between 0 and 1000",
                    "must be between 0.00 and " + Integer.MAX_VALUE + ".00"
                )
            );
        }

    }

    @Nested
    @DisplayName("changeProductStock endpoint tests")
    class ChangeProductStockTests {

        private final long productId = 1L;
        private final ModifyStockRequest request = new ModifyStockRequest(10);

        @Test
        @WithMockUser(roles = "OPERATOR")
        @DisplayName("Should return 200 OK when successfully changed product stock")
        void shouldReturn200_whenSuccessfullyModifiedProductStock() throws Exception {
            //Arrange
            Instant createdAt = Instant.now();
            String createdBy = "operator";
            String price = "99.99";
            ProductResponse response = new ProductResponse(1L, "product_name", null, 10, new BigDecimal(price), createdAt, createdBy, createdAt, createdBy);

            when(productService.changeProductStock(anyLong(), any(ModifyStockRequest.class)))
                .thenReturn(response);

            //Act and Assert
            ResultActions result = mockMvc.perform(
                    patch("/products/{productId}/stock", productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isOk());

            assertProductResponse(result, response);

            verify(productService).changeProductStock(anyLong(), any(ModifyStockRequest.class));
        }

        @Test
        @WithMockUser(roles = "CUSTOMER")
        @DisplayName("Should return 403 Forbidden when accessed with authentication but wrong role")
        void shouldReturn403_whenAuthenticatedWithWrongRole() throws Exception {
            //Act and Assert
            ResultActions result = mockMvc.perform(
                    patch("/products/{productId}/stock", productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isForbidden());

            assertForbiddenErrorResponse(result);
        }

        @Test
        @DisplayName("Should return 401 Unauthorized when accessed with no authentication")
        void shouldReturn401_whenNoAuthentication() throws Exception {
            //Act and Assert
            ResultActions result = mockMvc.perform(
                    patch("/products/{productId}/stock", productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isUnauthorized());

            assertNoAuthErrorResponse(result);
        }

        @ParameterizedTest
        @CsvSource(
            nullValues = "NULL",
            textBlock = """
                -1, NULL, must be greater than 0, must not be null
                -1, 0, must be greater than 0, must not be 0
                """)
        @WithMockUser(roles = "OPERATOR")
        @DisplayName("Should return 400 Bad Request when invalid modify stock request fields")
        void shouldReturn400_whenInvalidModifyStockRequest(
            long productId,
            Integer stockChange,
            String productIdMessage,
            String stockChangeMessage
        ) throws Exception {
            //Arrange
            ModifyStockRequest request = new ModifyStockRequest(stockChange);

            //Act and Assert
            ResultActions result = mockMvc.perform(
                    patch("/products/{productId}/stock", productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isBadRequest());

            assertSingleErrorValidationErrorResponse(result, Map.of("product_id", productIdMessage, "stock_change", stockChangeMessage));
        }

    }

    @Nested
    @DisplayName("getProduct endpoint tests")
    class GetProductTests {

        private final long productId = 1L;
        private final Instant createdAt = Instant.now();
        private final String createdBy = "operator";
        private final String price = "99.99";
        private final ProductResponse response = new ProductResponse(1L, "product_name", null, 10, new BigDecimal(price), createdAt, createdBy, createdAt, createdBy);

        @Test
        @WithMockUser(roles = "OPERATOR")
        @DisplayName("Should return 200 OK and product audit fields when accessed as OPERATOR")
        void shouldReturn200WithAuditing_whenAccessedAsOperator() throws Exception {
            //Arrange
            when(productService.getProduct(anyLong()))
                .thenReturn(response);
            when(securityUtils.getAuthenticatedUserRole())
                .thenReturn(User.UserRole.OPERATOR.name());

            //Act and Assert
            ResultActions result = mockMvc.perform(
                    get("/products/{productId}", productId)
                )
                .andExpect(status().isOk());

            assertProductResponse(result, response);

            verify(productService).getProduct(anyLong());
        }

        @ParameterizedTest
        @NullSource
        @ValueSource(strings = {"CUSTOMER"})
        @DisplayName("Should return 200 OK and no product audit fields when accessed not as OPERATOR")
        void shouldReturn200WithNoAuditing_whenAccessedNotAsOperator(String role) throws Exception {
            //Arrange
            when(productService.getProduct(anyLong()))
                .thenReturn(response);
            when(securityUtils.getAuthenticatedUserRole())
                .thenReturn(role);

            //Act and Assert
            ResultActions result = mockMvc.perform(
                    get("/products/{productId}", productId)
                )
                .andExpect(status().isOk());

            assertCustomerProductResponse(result, response);

            verify(productService).getProduct(anyLong());
        }

        @ParameterizedTest
        @CsvSource(
            textBlock = """
                -1, must be greater than 0
                """)
        @WithMockUser(roles = "OPERATOR")
        @DisplayName("Should return 400 Bad Request when invalid productId path variable")
        void shouldReturn400_whenInvalidProductId(
            long productId,
            String productIdMessage
        ) throws Exception {
            //Act and Assert
            ResultActions result = mockMvc.perform(
                    get("/products/{productId}", productId)
                )
                .andExpect(status().isBadRequest());

            assertSingleErrorValidationErrorResponse(result, Map.of("product_id", productIdMessage));
        }

    }

    @Nested
    @DisplayName("getProducts endpoint tests")
    class GetProductsTests {

        @Test
        @WithMockUser
        @DisplayName("Should return 200 OK when successfully gotten a page of products")
        void shouldReturn200_whenSuccessfullyGottenPageOfProducts() throws Exception {
            //Arrange
            String price = "55.55";
            ProductShortResponse productResponse1 = new ProductShortResponse(1L, "Original Product", 10, new BigDecimal(price));
            ProductShortResponse productResponse2 = new ProductShortResponse(2L, "Original Product 2", 5, new BigDecimal(price));
            PaginatedResponse<ProductShortResponse> response = new PaginatedResponse<>(0, 10, 1, 1, List.of(productResponse1, productResponse2));

            when(productService.getProducts(any(Pageable.class), any(ProductFiltering.class)))
                .thenReturn(response);

            //Act and Assert
            ResultActions result = mockMvc.perform(
                    get("/products")
                )
                .andExpect(status().isOk());

            assertPaginatedResponse(result, response);

            result
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].id").value(productResponse1.id()))
                .andExpect(jsonPath("$.content[1].id").value(productResponse2.id()));

            verify(productService).getProducts(any(Pageable.class), any(ProductFiltering.class));
        }

        @Test
        @DisplayName("Should return 200 OK when accessed with no authentication")
        void shouldReturn200_whenNoAuthentication() throws Exception {
            //Arrange
            PaginatedResponse<ProductShortResponse> response = new PaginatedResponse<>(0, 10, 1, 1, List.of());

            when(productService.getProducts(any(Pageable.class), any(ProductFiltering.class)))
                .thenReturn(response);

            //Act and Assert
            mockMvc.perform(
                    get("/products")
                )
                .andExpect(status().isOk());

            verify(productService).getProducts(any(Pageable.class), any(ProductFiltering.class));
        }

        @ParameterizedTest
        @MethodSource("provideInvalidPaginationAndFiltering")
        @WithMockUser(roles = "OPERATOR")
        @DisplayName("Should return 400 Bad Request when invalid modify stock request fields")
        void shouldReturn400_whenInvalidProductFiltering(
            MultiValueMap<String, String> queryParams,
            Map<String, String> errorMessages
        ) throws Exception {
            //Act and Assert
            ResultActions result = mockMvc.perform(
                    get("/products")
                        .queryParams(queryParams)
                )
                .andExpect(status().isBadRequest());

            assertSingleErrorValidationErrorResponse(result, errorMessages);
        }

            /*
                Test data provider methods
         */

        static Stream<Arguments> provideInvalidPaginationAndFiltering() {
            String longName = "r".repeat(101);

            return Stream.of(
                Arguments.of(
                    MultiValueMap.fromSingleValue(Map.of(
                        "name_contains", longName,
                        "min_stock", "-1",
                        "min_price", "-1",
                        "max_price", "-1"
                    )),
                    Map.of(
                        "name_contains", "size must be between 0 and 100",
                        "min_stock", "must be greater than or equal to 0",
                        "min_price", "must be between 0.00 and " + Integer.MAX_VALUE + ".00",
                        "max_price", "must be between 0.00 and " + Integer.MAX_VALUE + ".00"
                    )
                ),
                Arguments.of(
                    MultiValueMap.fromSingleValue(Map.of(
                        "name_contains", "   ",
                        "min_stock", "-1",
                        "min_price", Integer.MAX_VALUE + ".01",
                        "max_price", Integer.MAX_VALUE + ".01"
                    )),
                    Map.of(
                        "name_contains", "must be null or have at least one non-whitespace character",
                        "min_stock", "must be greater than or equal to 0",
                        "min_price", "must be between 0.00 and " + Integer.MAX_VALUE + ".00",
                        "max_price", "must be between 0.00 and " + Integer.MAX_VALUE + ".00"
                    )
                )
            );
        }

    }

    @Nested
    @DisplayName("deleteProduct endpoint tests")
    class DeleteProductTests {

        private final long productId = 1L;

        @Test
        @WithMockUser(roles = "OPERATOR")
        @DisplayName("Should return 204 No Content when successfully deleted product")
        void shouldReturn204_whenSuccessfullyModifiedProductStock() throws Exception {
            //Arrange
            doNothing()
                .when(productService).deleteProduct(anyLong());

            //Act and Assert
            ResultActions result = mockMvc.perform(
                    delete("/products/{productId}", productId)
                );

            assertNoContentResponse(result);

            verify(productService).deleteProduct(anyLong());
        }

        @Test
        @WithMockUser(roles = "CUSTOMER")
        @DisplayName("Should return 403 Forbidden when accessed with authentication but wrong role")
        void shouldReturn403_whenAuthenticatedWithWrongRole() throws Exception {
            //Act and Assert
            ResultActions result = mockMvc.perform(
                    delete("/products/{productId}", productId)
                )
                .andExpect(status().isForbidden());

            assertForbiddenErrorResponse(result);
        }

        @Test
        @DisplayName("Should return 401 Unauthorized when accessed with no authentication")
        void shouldReturn401_whenNoAuthentication() throws Exception {
            //Act and Assert
            ResultActions result = mockMvc.perform(
                    delete("/products/{productId}", productId)
                )
                .andExpect(status().isUnauthorized());

            assertNoAuthErrorResponse(result);
        }

        @ParameterizedTest
        @CsvSource(
            nullValues = "NULL",
            textBlock = """
                -1, must be greater than 0
                """)
        @WithMockUser(roles = "OPERATOR")
        @DisplayName("Should return 400 Bad Request when invalid productId path variable")
        void shouldReturn400_whenInvalidModifyStockRequest(
            long productId,
            String productIdMessage
        ) throws Exception {
            //Act and Assert
            ResultActions result = mockMvc.perform(
                    delete("/products/{productId}", productId)
                )
                .andExpect(status().isBadRequest());

            assertSingleErrorValidationErrorResponse(result, Map.of("product_id", productIdMessage));
        }

    }

}
