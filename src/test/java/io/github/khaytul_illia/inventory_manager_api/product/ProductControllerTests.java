package io.github.khaytul_illia.inventory_manager_api.product;

import io.github.khaytul_illia.inventory_manager_api.product.request.CreateProductRequest;
import io.github.khaytul_illia.inventory_manager_api.product.response.ProductResponse;
import io.github.khaytul_illia.inventory_manager_api.security.AuthenticationErrorHandler;
import io.github.khaytul_illia.inventory_manager_api.security.AuthorizationErrorHandler;
import io.github.khaytul_illia.inventory_manager_api.security.SecurityConfig;
import io.github.khaytul_illia.inventory_manager_api.security.jwt.JwtAuthenticationErrorHandler;
import io.github.khaytul_illia.inventory_manager_api.security.jwt.JwtConfig;
import io.github.khaytul_illia.inventory_manager_api.security.jwt.JwtRsaPemKeyConfig;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.stream.Stream;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
@Import({
    SecurityConfig.class,
    JwtRsaPemKeyConfig.class,
    JwtConfig.class,
    JwtAuthenticationErrorHandler.class,
    AuthenticationErrorHandler.class,
    AuthorizationErrorHandler.class
})
@ActiveProfiles("test")
@DisplayName("ProductController tests")
public class ProductControllerTests {

    @MockitoBean
    private ProductService productService;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Nested
    @DisplayName("createProduct endpoint tests")
    class CreateProductTests{

        @Test
        @WithMockUser(roles = "OPERATOR")
        @DisplayName("Should return 201 Created when successfully created new product")
        void shouldReturn201_whenSuccessfullyCreatedProduct() throws Exception{
            //Arrange
            Instant createdAt = Instant.now();
            String createdBy = "operator";
            String price = "99.99";
            CreateProductRequest request = new CreateProductRequest("product_name", null, new BigDecimal(price));
            ProductResponse response = new ProductResponse(1L, "product_name", "description", 0, new BigDecimal(price), createdAt, createdBy, createdAt, createdBy);

            when(productService.createProduct(request))
                .thenReturn(response);

            //Act and Assert
            mockMvc.perform(
                    post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", String.format("http://localhost/products/%s", response.id())))
                .andExpect(jsonPath("$.id").value(response.id()))
                .andExpect(jsonPath("$.name").value(response.name()))
                .andExpect(jsonPath("$.price").value(price))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.createdBy").exists())
                .andExpect(jsonPath("$.modifiedAt").exists())
                .andExpect(jsonPath("$.modifiedBy").exists());

            verify(productService).createProduct(request);
        }

        @Test
        @WithMockUser(roles = "CUSTOMER")
        @DisplayName("Should return 403 Forbidden when accessed with authentication but wrong´role")
        void shouldReturn403_whenAuthenticatedWithWrongRole() throws Exception {
            //Arrange
            String price = "99.99";
            CreateProductRequest request = new CreateProductRequest("product_name", null, new BigDecimal(price));

            //Act and Assert
            mockMvc.perform(
                    post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(HttpServletResponse.SC_FORBIDDEN));
        }

        @Test
        @DisplayName("Should return 401 Unauthorized when accessed with no authentication")
        void shouldReturn401_whenNoAuthentication() throws Exception {
            //Arrange
            String price = "99.99";
            CreateProductRequest request = new CreateProductRequest("product_name", null, new BigDecimal(price));

            //Act and Assert
            mockMvc.perform(
                    post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(HttpServletResponse.SC_UNAUTHORIZED));
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
        ) throws Exception{
            //Arrange
            CreateProductRequest request = new CreateProductRequest(name, description, price);

            //Act and Assert
            mockMvc.perform(
                    post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(HttpServletResponse.SC_BAD_REQUEST))
                .andExpect(jsonPath("$.data.name").value(nameMessage))
                .andExpect(jsonPath("$.data.description").value(descriptionMessage))
                .andExpect(jsonPath("$.data.price").value(priceMessage));
        }

        /*
                Test data provider methods
         */

        static Stream<Arguments> provideInvalidCreateProductRequests(){
            String longDescription = "r".repeat(1001);
            String longName = "r".repeat(101);

            return Stream.of(
                Arguments.of(
                    null, longDescription, null, "must not be blank", "size must be between 0 and 1000", "must not be null"
                ),
                Arguments.of(
                    "", longDescription, new BigDecimal(-1), "must not be blank", "size must be between 0 and 1000", "must be greater than or equal to 0"
                ),
                Arguments.of(
                    "   ", longDescription, new BigDecimal(Integer.MAX_VALUE + ".01"), "must not be blank", "size must be between 0 and 1000", "must be less than or equal to " + Integer.MAX_VALUE
                ),
                Arguments.of(
                    longName, longDescription, null, "size must be between 0 and 100", "size must be between 0 and 1000", "must not be null"
                )
            );
        }

    }

}
