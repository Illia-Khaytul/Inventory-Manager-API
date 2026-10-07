package io.github.khaytul_illia.inventory_manager_api.product;

import io.github.khaytul_illia.inventory_manager_api.TestcontainersConfiguration;
import io.github.khaytul_illia.inventory_manager_api.product.request.CreateProductRequest;
import io.github.khaytul_illia.inventory_manager_api.product.response.ProductResponse;
import io.github.khaytul_illia.inventory_manager_api.user.User;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@DisplayName("ProductService integration tests")
public class ProductServiceIT {

    @Autowired
    private ProductService productService;
    @Autowired
    private ProductRepository productRepository;

    private final String authenticatedUsername = "operator1";
    private final String productName = "original product";
    private final BigDecimal price = new BigDecimal("20.05");
    private Product product;

    @BeforeEach
    void beforeEach(){
        Jwt jwtMock = mock(Jwt.class);
        Authentication authentication = new TestingAuthenticationToken(jwtMock, "password", User.UserRole.OPERATOR.name());

        when(jwtMock.getSubject())
            .thenReturn(authenticatedUsername);

        SecurityContextHolder.getContext().setAuthentication(authentication);

        product = new Product();
        product.setName(productName);
        product.setStock(100);
        product.setPrice(price);

        productRepository.save(product);
    }

    @AfterEach
    void afterEach(){
        SecurityContextHolder.getContext().setAuthentication(null);

        productRepository.deleteAll();
    }

    @Nested
    @DisplayName("createProduct integration tests")
    class CreateProductIT{

        @Test
        @DisplayName("Should create new product with the provided data when the product name is unique")
        void shouldCreateNewProduct_whenProductNameIsUnique(){
            //Arrange
            CreateProductRequest request = new CreateProductRequest(productName + "_unique", null, price);

            //Act
            ProductResponse response = productService.createProduct(request);

            //Assert
            Instant now = Instant.now();

            assertThat(response).isNotNull();
            assertThat(response.id()).isNotNull();
            assertThat(response.stock()).isEqualTo(0);
            assertThat(response.createdAt()).isCloseTo(now, within(10, ChronoUnit.SECONDS));
            assertThat(response.createdBy()).isEqualTo(authenticatedUsername);
            assertThat(response.modifiedAt()).isNull();
            assertThat(response.modifiedBy()).isNull();
            assertThat(productRepository.count()).isEqualTo(2);
        }

    }

}
