package io.github.khaytul_illia.inventory_manager_api.product;

import io.github.khaytul_illia.inventory_manager_api.error.exception.EntityNotFoundException;
import io.github.khaytul_illia.inventory_manager_api.support.context.ServiceITContext;
import io.github.khaytul_illia.inventory_manager_api.common.response.pagination.PaginatedResponse;
import io.github.khaytul_illia.inventory_manager_api.product.request.CreateProductRequest;
import io.github.khaytul_illia.inventory_manager_api.product.request.ModifyStockRequest;
import io.github.khaytul_illia.inventory_manager_api.product.request.ProductFiltering;
import io.github.khaytul_illia.inventory_manager_api.product.request.UpdateProductRequest;
import io.github.khaytul_illia.inventory_manager_api.product.response.ProductResponse;
import io.github.khaytul_illia.inventory_manager_api.product.response.ProductShortResponse;
import io.github.khaytul_illia.inventory_manager_api.support.data.ProductTestDataFactory;
import io.github.khaytul_illia.inventory_manager_api.support.data.TestDataConstants;
import io.github.khaytul_illia.inventory_manager_api.support.utils.TestAuthenticationUtils;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.TransactionDefinition;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static io.github.khaytul_illia.inventory_manager_api.support.utils.TestAuthenticationUtils.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("ProductService integration tests")
public class ProductServiceIT extends ServiceITContext {

    @Autowired
    private ProductService productService;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    @Qualifier("productRepository")
    private ProductRepository realProductRepository;

    private Product product;

    @BeforeEach
    void beforeEach() {
        TestAuthenticationUtils.setOperatorAuthentication();

        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        product = productRepository.save(ProductTestDataFactory.defaults().stock(100).build());
    }

    @AfterEach
    void afterEach() {
        productRepository.deleteAll();

        TestAuthenticationUtils.resetAuthentication();
    }

    @Nested
    @DisplayName("createProduct integration tests")
    class CreateProductIT {

        @Test
        @DisplayName("Should create new product with the provided data")
        void shouldCreateNewProduct() {
            //Arrange
            CreateProductRequest request = ProductTestDataFactory.uniqueCreateProductRequest();

            //Act
            ProductResponse response = productService.createProduct(request);

            //Assert
            Instant now = Instant.now();

            assertThat(response).isNotNull();
            assertThat(response.id()).isNotNull();
            assertThat(response.stock()).isEqualTo(0);
            assertThat(response.createdAt()).isCloseTo(now, within(10, ChronoUnit.SECONDS));
            assertThat(response.createdBy()).isEqualTo(TestDataConstants.USER_USERNAME);
            assertThat(response.modifiedAt()).isNull();
            assertThat(response.modifiedBy()).isNull();
            assertThat(response.deletedAt()).isNull();
            assertThat(response.deletedBy()).isNull();

            assertThat(productRepository.count()).isEqualTo(2);
        }

    }

    @Nested
    @DisplayName("updateProduct integration tests")
    class UpdateProductIT {

        private final UpdateProductRequest request = ProductTestDataFactory.DEFAULT_UPDATE_REQUEST;

        @Test
        @DisplayName("Should throw EntityNotFoundException when product is deleted mid-operation")
        void shouldThrowEntityNotFoundException_whenProductDeletedConcurrently() {
            //Arrange
            doAnswer(invocation -> {
                Optional<Product> foundProduct = realProductRepository.findByIdAndDeletedAtNull(product.getId());

                transactionTemplate.executeWithoutResult(
                    status -> productRepository.deleteProductById(product.getId(), TestDataConstants.USER_USERNAME)
                );

                return foundProduct;
            })
                .when(productRepository).findByIdAndDeletedAtNull(product.getId());

            //Act and Assert
            assertThatThrownBy(() -> productService.updateProduct(product.getId(), request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage(String.format("Product with id %s does not exist", product.getId()));

            Product modifiedProduct = productRepository.findById(product.getId()).orElseThrow();
            assertThat(modifiedProduct.getName()).isEqualTo(TestDataConstants.PRODUCT_NAME);
            assertThat(modifiedProduct.getDescription()).isEqualTo(TestDataConstants.PRODUCT_DESCRIPTION);
            assertThat(modifiedProduct.getPrice()).isEqualTo(TestDataConstants.PRODUCT_PRICE);
        }

        @Test
        @DisplayName("Should throw OptimisticLockingFailureException when product is modified concurrently too many times")
        void shouldThrowOptimisticLockingFailureException_whenProductModifiedConcurrentlyTooMuch() {
            //Arrange
            doAnswer(invocation -> {
                Optional<Product> foundProduct = realProductRepository.findByIdAndDeletedAtNull(product.getId());

                transactionTemplate.executeWithoutResult(status -> {
                    Product target = realProductRepository.findById(product.getId()).orElseThrow();

                    target.setStock(target.getStock() + 1);
                });

                return foundProduct;
            })
                .when(productRepository).findByIdAndDeletedAtNull(product.getId());

            //Act and Assert
            assertThatThrownBy(() -> productService.updateProduct(product.getId(), request))
                .isInstanceOf(OptimisticLockingFailureException.class);

            Product modifiedProduct = productRepository.findById(product.getId()).orElseThrow();
            assertThat(modifiedProduct.getName()).isEqualTo(TestDataConstants.PRODUCT_NAME);
            assertThat(modifiedProduct.getDescription()).isEqualTo(TestDataConstants.PRODUCT_DESCRIPTION);
            assertThat(modifiedProduct.getPrice()).isEqualTo(TestDataConstants.PRODUCT_PRICE);
        }

        @Test
        @DisplayName("Should update product when product is modified concurrently once")
        void shouldUpdateProduct_whenProductModifiedConcurrentlyOnce() {
            //Arrange
            doAnswer(invocation -> {
                Optional<Product> foundProduct = realProductRepository.findByIdAndDeletedAtNull(product.getId());

                transactionTemplate.executeWithoutResult(status -> {
                    Product target = realProductRepository.findById(product.getId()).orElseThrow();

                    target.setStock(target.getStock() + 1);
                });

                return foundProduct;
            })
                .doAnswer(invocation -> realProductRepository.findByIdAndDeletedAtNull(product.getId()))
                .when(productRepository).findByIdAndDeletedAtNull(product.getId());

            //Act
            ProductResponse response = productService.updateProduct(product.getId(), request);

            //Assert
            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(product.getId());

            Product modifiedProduct = productRepository.findById(product.getId()).orElseThrow();
            assertThat(modifiedProduct.getName()).isEqualTo(request.name());
            assertThat(modifiedProduct.getDescription()).isEqualTo(TestDataConstants.PRODUCT_DESCRIPTION);
            assertThat(modifiedProduct.getPrice()).isEqualTo(request.price());
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
            assertThat(response.name()).isEqualTo(request.name());
            assertThat(response.description()).isEqualTo(TestDataConstants.PRODUCT_DESCRIPTION);
            assertThat(response.price()).isEqualTo(request.price());
            assertThat(response.createdAt()).isCloseTo(now, within(1, ChronoUnit.MINUTES));
            assertThat(response.createdBy()).isEqualTo(TestDataConstants.USER_USERNAME);
            assertThat(response.modifiedAt()).isCloseTo(now, within(1, ChronoUnit.MINUTES));
            assertThat(response.modifiedBy()).isEqualTo(TestDataConstants.USER_USERNAME);
            assertThat(response.deletedAt()).isNull();
            assertThat(response.deletedBy()).isNull();

            Product modifiedProduct = productRepository.findById(product.getId()).orElseThrow();
            assertThat(modifiedProduct.getName()).isEqualTo(request.name());
            assertThat(modifiedProduct.getDescription()).isEqualTo(TestDataConstants.PRODUCT_DESCRIPTION);
            assertThat(modifiedProduct.getPrice()).isEqualTo(request.price());
        }

    }

    @Nested
    @DisplayName("changeProductStock integration tests")
    class ChangeProductStockIT {

        @Test
        @DisplayName("Should change product stock")
        void shouldChangeProductStock() {
            //Arrange
            ModifyStockRequest request = ProductTestDataFactory.DEFAULT_MODIFY_STOCK_REQUEST;
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
            assertThat(response.createdBy()).isEqualTo(TestDataConstants.USER_USERNAME);
            assertThat(response.modifiedAt()).isCloseTo(now, within(1, ChronoUnit.MINUTES));
            assertThat(response.modifiedBy()).isEqualTo(TestDataConstants.USER_USERNAME);
            assertThat(response.deletedAt()).isNull();
            assertThat(response.deletedBy()).isNull();

            Product modifiedProduct = productRepository.findById(product.getId()).orElseThrow();
            assertThat(modifiedProduct.getStock()).isEqualTo(originalStock + request.stockChange());
            assertThat(modifiedProduct.getVersion()).isEqualTo(originalVersion + 1);
        }

    }

    @Nested
    @DisplayName("getProduct integration tests")
    class GetProductIT {

        @Test
        @DisplayName("Should get product by id when accessed not as OPERATOR")
        void shouldGetProductById_whenAccessedNotAsOperator() {
            //Arrange
            TestAuthenticationUtils.setCustomerAuthentication();

            //Act
            ProductResponse response = productService.getProduct(product.getId());

            //Assert
            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(product.getId());
            assertThat(response.name()).isEqualTo(product.getName());
        }

        @Test
        @DisplayName("Should get deleted product by id when accessed as OPERATOR")
        void shouldGetDeletedProduct_whenAccessedAsOperator() {
            //Arrange
            transactionTemplate.executeWithoutResult(
                status -> productRepository.deleteProductById(product.getId(), TestDataConstants.USER_USERNAME)
            );

            //Act
            ProductResponse response = productService.getProduct(product.getId());

            //Assert
            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(product.getId());
            assertThat(response.name()).isEqualTo(product.getName());
        }

    }

    @Nested
    @DisplayName("getProducts integration tests")
    class GetProductsIT {

        private List<Product> products;

        @BeforeEach
        void beforeEach() {
            Product product1 = productRepository.save(ProductTestDataFactory.newProduct("product aaa", 0, "10"));
            Product product2 = productRepository.save(ProductTestDataFactory.newProduct("product aba", 10, "10.01"));
            Product product3 = productRepository.save(ProductTestDataFactory.newProduct("product abc", 5, "20"));
            Product product4 = productRepository.save(ProductTestDataFactory.newProduct("product acb", 6, "5.6"));
            Product product5 = productRepository.save(ProductTestDataFactory.newProduct("product cba", 7, "9.3"));
            Product product6 = productRepository.save(ProductTestDataFactory.defaults().name("product aab").stock(8).deleted().build());
            Product product7 = productRepository.save(ProductTestDataFactory.defaults().name("product aca").stock(3).deleted().build());
            products = List.of(product1, product2, product3, product4, product5, product6, product7);

            productRepository.deleteById(product.getId());
        }

        @ParameterizedTest
        @MethodSource("providePaginationAndFiltering")
        @DisplayName("Should get not deleted products with pagination and filtering when accessed not as operator")
        void shouldGetNotDeletedProducts_whenAccessedNotAsOperator(Authentication authentication, Pageable pagination, ProductFiltering filtering, int[] expected, int totalElements) {
            //Arrange
            setAuthentication(authentication);

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
                //No filtering, big pagination, for customer
                Arguments.of(
                    buildCustomerAuthentication(),
                    PageRequest.of(0, 100),
                    new ProductFiltering(null, null, null, null, false),
                    new int[]{0, 1, 2, 3, 4},
                    5
                ),
                //No filtering, small pagination, for customer
                Arguments.of(
                    buildCustomerAuthentication(),
                    PageRequest.of(0, 2),
                    new ProductFiltering(null, null, null, null, false),
                    new int[]{0, 1},
                    5
                ),
                //Show deleted filtering, for customer
                Arguments.of(
                    buildCustomerAuthentication(),
                    PageRequest.of(0, 10),
                    new ProductFiltering(null, null, null, null, true),
                    new int[]{0, 1, 2, 3, 4},
                    5
                ),
                //Min stock and max price filtering, for customer
                Arguments.of(
                    buildCustomerAuthentication(),
                    PageRequest.of(0, 10),
                    new ProductFiltering(null, 6, null, new BigDecimal("10"), false),
                    new int[]{3, 4},
                    2
                ),
                //Name contains and min price filtering, small pagination and stock sorting, for customer
                Arguments.of(
                    buildCustomerAuthentication(),
                    PageRequest.of(0, 2, Sort.by(Sort.Direction.ASC, "stock")),
                    new ProductFiltering("b", null, new BigDecimal("9"), null, false),
                    new int[]{2, 4},
                    3
                ),
                //Show deleted filtering, for operator
                Arguments.of(
                    buildOperatorAuthentication(),
                    PageRequest.of(0, 10),
                    new ProductFiltering(null, null, null, null, true),
                    new int[]{0, 1, 2, 3, 4, 5, 6},
                    7
                ),
                //Show deleted and min stock filtering, name sorting, for operator
                Arguments.of(
                    buildOperatorAuthentication(),
                    PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "name")),
                    new ProductFiltering(null, 5, null, null, true),
                    new int[]{5, 1, 2, 3, 4},
                    5
                )
            );
        }

    }

    @Nested
    @DisplayName("deleteProduct integration tests")
    class DeleteProductIT {

        @Test
        @DisplayName("Should delete product by id")
        void shouldDeleteProductById(){
            //Act
            productService.deleteProduct(product.getId());

            //Assert
            Product foundProduct = productRepository.findById(product.getId()).orElseThrow();
            assertThat(foundProduct.getDeletedAt()).isCloseTo(Instant.now(), within(10, ChronoUnit.SECONDS));
            assertThat(foundProduct.getDeletedBy()).isEqualTo(TestDataConstants.USER_USERNAME);
        }

    }

}
