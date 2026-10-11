package io.github.khaytul_illia.inventory_manager_api.product;

import io.github.khaytul_illia.inventory_manager_api.TestcontainersConfiguration;
import io.github.khaytul_illia.inventory_manager_api.common.response.pagination.PaginatedResponse;
import io.github.khaytul_illia.inventory_manager_api.error.exception.EntityNotFoundException;
import io.github.khaytul_illia.inventory_manager_api.product.request.CreateProductRequest;
import io.github.khaytul_illia.inventory_manager_api.product.request.ModifyStockRequest;
import io.github.khaytul_illia.inventory_manager_api.product.request.ProductFiltering;
import io.github.khaytul_illia.inventory_manager_api.product.request.UpdateProductRequest;
import io.github.khaytul_illia.inventory_manager_api.product.response.ProductResponse;
import io.github.khaytul_illia.inventory_manager_api.product.response.ProductShortResponse;
import io.github.khaytul_illia.inventory_manager_api.user.User;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

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
    void beforeEach() {
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
    void afterEach() {
        SecurityContextHolder.getContext().setAuthentication(null);

        productRepository.deleteAll();
    }

    @Nested
    @DisplayName("createProduct integration tests")
    class CreateProductIT {

        @Test
        @DisplayName("Should create new product with the provided data")
        void shouldCreateNewProduct() {
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
    class UpdateProductIT {

        private final String newName = productName + "_new";
        private final BigDecimal newPrice = new BigDecimal("101.99");
        private final UpdateProductRequest request = new UpdateProductRequest(newName, null, newPrice);

        @Test
        @DisplayName("Should throw EntityNotFoundException when product is deleted mid-operation")
        void shouldThrowEntityNotFoundException_whenProductDeletedConcurrently() {
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
        void shouldThrowOptimisticLockingFailureException_whenProductModifiedConcurrentlyTooMuch() {
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
        void shouldUpdateProduct_whenProductModifiedConcurrentlyOnce() {
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
        void shouldUpdateProduct_whenProductNotModifiedConcurrently() {
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

    @Nested
    @DisplayName("changeProductStock integration tests")
    class ChangeProductStockIT {

        @Test
        @DisplayName("Should change product stock")
        void shouldChangeProductStock() {
            //Arrange
            ModifyStockRequest request = new ModifyStockRequest(10);
            int originalStock = product.getStock();
            int originalVersion = product.getVersion();

            //Act
            ProductResponse response = productService.changeProductStock(product.getId(), request);

            //Assert
            Instant now = Instant.now();
            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(product.getId());
            assertThat(response.stock()).isEqualTo(originalStock + request.stockChange());
            assertThat(response.createdAt()).isCloseTo(now, within(1, ChronoUnit.MINUTES));
            assertThat(response.createdBy()).isEqualTo(authenticatedUsername);
            assertThat(response.modifiedAt()).isCloseTo(now, within(1, ChronoUnit.MINUTES));
            assertThat(response.modifiedBy()).isEqualTo(authenticatedUsername);

            Product modifiedProduct = productRepository.findById(product.getId()).orElseThrow();
            assertThat(modifiedProduct.getStock()).isEqualTo(originalStock + request.stockChange());
            assertThat(modifiedProduct.getVersion()).isEqualTo(originalVersion + 1);
        }

    }

    @Nested
    @DisplayName("getProduct integration tests")
    class GetProductIT {

        @Test
        @DisplayName("Should get product by id")
        void shouldGetProductById() {
            //Act
            ProductResponse response = productService.getProduct(product.getId());

            //Assert
            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(product.getId());
            assertThat(response.name()).isEqualTo(productName);
        }

    }

    @Nested
    @DisplayName("getProducts integration tests")
    class GetProductsIT {

        private List<Product> products;

        @BeforeEach
        void beforeEach() {
            Instant now = Instant.now();
            Product product1 = productRepository.save(new Product(null, "product1aaa", null, 0, new BigDecimal("10"), now, "operator1", null, null, null));
            Product product2 = productRepository.save(new Product(null, "product2aba", null, 10, new BigDecimal("10.01"), now, "operator1", null, null, null));
            Product product3 = productRepository.save(new Product(null, "product3abc", null, 5, new BigDecimal("20"), now, "operator1", null, null, null));
            Product product4 = productRepository.save(new Product(null, "product4acb", null, 6, new BigDecimal("5.6"), now, "operator1", null, null, null));
            Product product5 = productRepository.save(new Product(null, "product5cba", null, 7, new BigDecimal("9.3"), now, "operator1", null, null, null));
            products = List.of(product1, product2, product3, product4, product5);

            productRepository.deleteById(product.getId());
        }

        @ParameterizedTest
        @MethodSource("providePaginationAndFiltering")
        @DisplayName("Should get products with pagination and filtering")
        void shouldGetProductsWithPaginationAndFiltering(Pageable pagination, ProductFiltering filtering, int[] expected, int totalElements) {
            //Act
            PaginatedResponse<ProductShortResponse> response = productService.getProducts(pagination, filtering);

            //Assert
            assertThat(response).isNotNull();
            assertThat(response.page()).isEqualTo(pagination.getPageNumber());
            assertThat(response.pageSize()).isEqualTo(pagination.getPageSize());
            assertThat(response.totalPages()).isEqualTo(totalElements / pagination.getPageSize() + (totalElements % pagination.getPageSize() == 0 ? 0 : 1));
            assertThat(response.totalElements()).isEqualTo(totalElements);
            assertThat(response.content()).hasSize(expected.length);
            assertThat(
                response.content().stream().map(ProductShortResponse::id).toList()
            ).containsExactlyElementsOf(
                Arrays.stream(expected).mapToObj(i -> products.get(i).getId()).toList()
            );
        }

        /*
               Test data provider methods
        */

        static Stream<Arguments> providePaginationAndFiltering() {
            return Stream.of(
                //No filtering, big pagination
                Arguments.of(
                    PageRequest.of(0, 100),
                    new ProductFiltering(null, null, null, null),
                    new int[]{0, 1, 2, 3, 4},
                    5
                ),
                //No filtering, small pagination
                Arguments.of(
                    PageRequest.of(0, 2),
                    new ProductFiltering(null, null, null, null),
                    new int[]{0, 1},
                    5
                ),
                //Name contains filtering
                Arguments.of(
                    PageRequest.of(0, 10),
                    new ProductFiltering("ab", null, null, null),
                    new int[]{1, 2},
                    2
                ),
                //Min stock filtering
                Arguments.of(
                    PageRequest.of(0, 10),
                    new ProductFiltering(null, 6, null, null),
                    new int[]{1, 3, 4},
                    3
                ),
                //Min price filtering and sorting
                Arguments.of(
                    PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "stock")),
                    new ProductFiltering(null, null, new BigDecimal("10"), null),
                    new int[]{0, 2, 1},
                    3
                ),
                //Max price filtering
                Arguments.of(
                    PageRequest.of(0, 10),
                    new ProductFiltering(null, null, null, new BigDecimal("10")),
                    new int[]{0, 3, 4},
                    3
                ),
                //Multiple filtering, small pagination and stock sorting
                Arguments.of(
                    PageRequest.of(0, 2, Sort.by(Sort.Direction.ASC, "stock")),
                    new ProductFiltering("b", null, new BigDecimal("9"), null),
                    new int[]{2, 4},
                    3
                )
            );
        }

    }

}
