package io.github.khaytul_illia.inventory_manager_api.product;

import io.github.khaytul_illia.inventory_manager_api.TestcontainersConfiguration;
import io.github.khaytul_illia.inventory_manager_api.error.exception.EntityNotFoundException;
import io.github.khaytul_illia.inventory_manager_api.product.request.CreateProductRequest;
import io.github.khaytul_illia.inventory_manager_api.product.request.UpdateProductRequest;
import io.github.khaytul_illia.inventory_manager_api.product.response.ProductResponse;
import io.github.khaytul_illia.inventory_manager_api.user.User;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@DisplayName("ProductService integration tests")
public class ProductServiceIT {

    @MockitoSpyBean
    private ProductMapper productMapper;

    @Autowired
    private ProductService productService;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private TransactionTemplate transactionTemplate;

    private final String authenticatedUsername = "operator1";
    private final String productName = "original product";
    private final BigDecimal price = new BigDecimal("20.05");
    private Product product;

    @BeforeEach
    void beforeEach(){
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        Jwt jwtMock = mock(Jwt.class);
        SecurityContextHolder.getContext().setAuthentication(
            new TestingAuthenticationToken(jwtMock, "password", User.UserRole.OPERATOR.name())
        );

        when(jwtMock.getSubject())
            .thenReturn(authenticatedUsername);

        product = productRepository.save(new Product(null, productName, null, 100, price, null, null, null, null, null));
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

    @Nested
    @DisplayName("updateProduct integration tests")
    class UpdateProductIT{

        private final String newName = productName + "_new";
        private final BigDecimal newPrice = new BigDecimal("101.99");
        private final UpdateProductRequest request = new UpdateProductRequest(newName, null, newPrice);

        @Test
        @DisplayName("Should throw EntityNotFoundException when product is deleted mid-operation")
        void shouldThrowEntityNotFoundException_whenProductDeletedConcurrently(){
            //Arrange
            doAnswer(invocation -> {
                transactionTemplate.executeWithoutResult(status -> productRepository.deleteById(product.getId()));

                return invocation.callRealMethod();
            })
                .when(productMapper).updateProduct(any(Product.class), any(UpdateProductRequest.class));

            //Act and Assert
            assertThatThrownBy(() -> productService.updateProduct(product.getId(), request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage(String.format("Product with id %s does not exist", product.getId()));

            assertThat(productRepository.count()).isEqualTo(0);
        }

        @Test
        @DisplayName("Should throw OptimisticLockingFailureException when product is modified concurrently too many times")
        void shouldThrowOptimisticLockingFailureException_whenProductModifiedConcurrentlyTooMuch(){
            //Arrange
            doAnswer(invocation -> {
                transactionTemplate.executeWithoutResult(status -> {
                    Product target = productRepository.findById(product.getId()).orElseThrow();

                    target.setStock(target.getStock() + 1);
                });

                return invocation.callRealMethod();
            })
                .when(productMapper).updateProduct(any(Product.class), any(UpdateProductRequest.class));

            //Act and Assert
            assertThatThrownBy(() -> productService.updateProduct(product.getId(), request))
                .isInstanceOf(OptimisticLockingFailureException.class);

            Product modifiedProduct = productRepository.findById(product.getId()).orElseThrow();
            assertThat(modifiedProduct.getName()).isEqualTo(productName);
            assertThat(modifiedProduct.getPrice()).isEqualTo(price);
        }

        @Test
        @DisplayName("Should update product when product is modified concurrently once")
        void shouldUpdateProduct_whenProductModifiedConcurrentlyOnce(){
            //Arrange
            doAnswer(invocation -> {
                transactionTemplate.executeWithoutResult(status -> {
                    Product target = productRepository.findById(product.getId()).orElseThrow();

                    target.setStock(target.getStock() + 1);
                });

                return invocation.callRealMethod();
            })
                .doCallRealMethod()
                .when(productMapper).updateProduct(any(Product.class), any(UpdateProductRequest.class));

            //Act
            ProductResponse response = productService.updateProduct(product.getId(), request);

            //Assert
            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(product.getId());

            Product modifiedProduct = productRepository.findById(product.getId()).orElseThrow();
            assertThat(modifiedProduct.getName()).isEqualTo(newName);
            assertThat(modifiedProduct.getPrice()).isEqualTo(newPrice);
        }

        @Test
        @DisplayName("Should update product when product is not modified concurrently")
        void shouldUpdateProduct_whenProductNotModifiedConcurrently(){
            //Arrange
            Instant now = Instant.now();

            //Act
            ProductResponse response = productService.updateProduct(product.getId(), request);

            //Assert
            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(product.getId());
            assertThat(response.name()).isEqualTo(newName);
            assertThat(response.price()).isEqualTo(newPrice);
            assertThat(response.createdAt()).isCloseTo(now, within(1, ChronoUnit.MINUTES));
            assertThat(response.createdBy()).isEqualTo(authenticatedUsername);
            assertThat(response.modifiedAt()).isCloseTo(now, within(1, ChronoUnit.MINUTES));
            assertThat(response.modifiedBy()).isEqualTo(authenticatedUsername);

            Product modifiedProduct = productRepository.findById(product.getId()).orElseThrow();
            assertThat(modifiedProduct.getName()).isEqualTo(newName);
            assertThat(modifiedProduct.getPrice()).isEqualTo(newPrice);
        }

    }

}
