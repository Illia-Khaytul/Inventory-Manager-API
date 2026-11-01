package io.github.khaytul_illia.inventory_manager_api.product;

import io.github.khaytul_illia.inventory_manager_api.common.response.pagination.PaginatedResponse;
import io.github.khaytul_illia.inventory_manager_api.error.exception.DuplicateEntryException;
import io.github.khaytul_illia.inventory_manager_api.error.exception.EntityNotFoundException;
import io.github.khaytul_illia.inventory_manager_api.error.exception.InvalidStockModificationException;
import io.github.khaytul_illia.inventory_manager_api.product.request.CreateProductRequest;
import io.github.khaytul_illia.inventory_manager_api.product.request.ModifyStockRequest;
import io.github.khaytul_illia.inventory_manager_api.product.request.ProductFiltering;
import io.github.khaytul_illia.inventory_manager_api.product.request.UpdateProductRequest;
import io.github.khaytul_illia.inventory_manager_api.product.response.ProductResponse;
import io.github.khaytul_illia.inventory_manager_api.product.response.ProductShortResponse;
import io.github.khaytul_illia.inventory_manager_api.security.SecurityUtils;
import io.github.khaytul_illia.inventory_manager_api.support.data.ProductTestDataFactory;
import io.github.khaytul_illia.inventory_manager_api.support.data.TestDataConstants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductService tests")
public class ProductServiceTests {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private ProductSpecificationBuilder specificationBuilder;
    @Mock
    private ProductMapper productMapper;
    @Mock
    private SecurityUtils securityUtils;
    @InjectMocks
    private ProductService productService;

    @Nested
    @DisplayName("createProduct tests")
    class CreateProductTests {

        private final CreateProductRequest request = ProductTestDataFactory.DEFAULT_CREATE_REQUEST;

        @Test
        @DisplayName("Should throw DuplicateEntryException when product name is not unique")
        void shouldThrowDuplicateEntryException_whenProductNameIsNotUnique() {
            //Arrange
            when(productRepository.existsByName(request.name()))
                .thenReturn(true);

            //Act and Assert
            assertThatThrownBy(() -> productService.createProduct(request))
                .isInstanceOf(DuplicateEntryException.class)
                .hasMessage(String.format("Product with name '%s' already exists", request.name()));

            verify(productRepository).existsByName(request.name());
        }

        @Test
        @DisplayName("Should create new product when product name is unique")
        void shouldCreateNewProduct_whenProductNameIsUnique() {
            //Arrange
            Product product = ProductTestDataFactory.existingDefaultProduct();
            ProductResponse expectedResponse = ProductTestDataFactory.DEFAULT_RESPONSE;

            when(productRepository.existsByName(request.name()))
                .thenReturn(false);
            when(productMapper.buildProduct(request))
                .thenReturn(product);
            when(productRepository.save(product))
                .thenReturn(product);
            when(productMapper.toResponse(product))
                .thenReturn(expectedResponse);

            //Act
            ProductResponse response = productService.createProduct(request);

            //Assert
            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(expectedResponse.id());
            assertThat(response.name()).isEqualTo(expectedResponse.name());

            verify(productRepository).existsByName(request.name());
            verify(productMapper).buildProduct(request);
            verify(productRepository).save(product);
            verify(productMapper).toResponse(product);
        }

    }

    @Nested
    @DisplayName("updateProduct tests")
    class UpdateProductTests {

        private final long productId = 1L;
        private final UpdateProductRequest request = ProductTestDataFactory.DEFAULT_UPDATE_REQUEST;

        @Test
        @DisplayName("Should throw EntityNotFoundException when product does not exist by id")
        void shouldThrowEntityNotFoundException_whenProductDoesNotExist() {
            //Arrange
            when(productRepository.findById(productId))
                .thenReturn(Optional.empty());

            //Act and Assert
            assertThatThrownBy(() -> productService.updateProduct(productId, request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage(String.format("Product with id %s does not exist", productId));

            verify(productRepository).findById(productId);
        }

        @Test
        @DisplayName("Should return product response when update request is empty")
        void shouldReturnProductResponse_whenRequestIsEmpty() {
            //Arrange
            UpdateProductRequest request = new UpdateProductRequest(null, null, null);
            Product product = ProductTestDataFactory.existingDefaultProduct();
            ProductResponse productResponse = ProductTestDataFactory.DEFAULT_RESPONSE;

            when(productRepository.findById(productId))
                .thenReturn(Optional.of(product));
            when(productMapper.toResponse(product))
                .thenReturn(productResponse);

            //Act
            ProductResponse response = productService.updateProduct(productId, request);

            //Assert
            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(productResponse.id());
            assertThat(response.name()).isEqualTo(productResponse.name());

            verify(productRepository).findById(productId);
            verify(productMapper).toResponse(product);
        }

        @Test
        @DisplayName("Should throw DuplicateEntryException when new product name is not unique")
        void shouldThrowDuplicateEntryException_whenNewProductNameIsNotUnique() {
            //Arrange
            Product product = ProductTestDataFactory.existingDefaultProduct();

            when(productRepository.findById(productId))
                .thenReturn(Optional.of(product));
            when(productRepository.existsByName(request.name()))
                .thenReturn(true);

            //Act and Assert
            assertThatThrownBy(() -> productService.updateProduct(productId, request))
                .isInstanceOf(DuplicateEntryException.class)
                .hasMessage(String.format("Product with name '%s' already exists", request.name()));

            verify(productRepository).findById(productId);
            verify(productRepository).existsByName(request.name());
        }

        @ParameterizedTest
        @CsvSource(
            nullValues = "NULL",
            textBlock = """
                original name, NULL
                original name, original name
                """)
        @DisplayName("Should not throw DuplicateEntryException when new product name is null or equal to old product name")
        void shouldNotThrowDuplicateEntryException_whenNewProductNameIsNullOrEqualToOldName(String oldName, String newName) {
            //Arrange
            UpdateProductRequest request = new UpdateProductRequest(newName, "placeholder", null);
            Product product = ProductTestDataFactory.defaults().name(oldName).build();
            ProductResponse productResponse = ProductTestDataFactory.defaults().id(1L).name(newName).toResponse();

            when(productRepository.findById(productId))
                .thenReturn(Optional.of(product));
            doNothing()
                .when(productMapper).updateProduct(product, request);
            when(productRepository.saveAndFlush(product))
                .thenReturn(product);
            when(productMapper.toResponse(product))
                .thenReturn(productResponse);

            //Act
            ProductResponse response = productService.updateProduct(productId, request);

            //Assert
            assertThat(response).isNotNull();

            verify(productRepository).findById(productId);
            verify(productRepository, never()).existsByName(request.name());
            verify(productMapper).updateProduct(product, request);
            verify(productMapper).toResponse(product);
        }

        @Test
        @DisplayName("Should update the found product when the new product name is unique")
        void shouldUpdateProduct_whenNewProductNameIsUnique() {
            //Arrange
            Product product = ProductTestDataFactory.existingDefaultProduct();
            ProductResponse productResponse = ProductTestDataFactory.DEFAULT_RESPONSE;

            when(productRepository.findById(productId))
                .thenReturn(Optional.of(product));
            when(productRepository.existsByName(request.name()))
                .thenReturn(false);
            doNothing()
                .when(productMapper).updateProduct(product, request);
            when(productRepository.saveAndFlush(product))
                .thenReturn(product);
            when(productMapper.toResponse(product))
                .thenReturn(productResponse);

            //Act
            ProductResponse response = productService.updateProduct(productId, request);

            //Assert
            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(productResponse.id());
            assertThat(response.name()).isEqualTo(productResponse.name());

            verify(productRepository).findById(productId);
            verify(productRepository).existsByName(request.name());
            verify(productMapper).updateProduct(product, request);
            verify(productMapper).toResponse(product);
        }

    }

    @Nested
    @DisplayName("changeProductStock tests")
    class ChangeProductStockTests {

        private final long productId = 1L;
        private final ModifyStockRequest request = ProductTestDataFactory.DEFAULT_MODIFY_STOCK_REQUEST;

        @Test
        @DisplayName("Should throw EntityNotFoundException when product does not exist by id")
        void shouldThrowEntityNotFoundException_whenProductDoesNotExist() {
            //Arrange
            when(securityUtils.getAuthenticatedUsername())
                .thenReturn(TestDataConstants.USER_USERNAME);
            when(productRepository.changeProductStock(anyLong(), anyInt(), any(Instant.class), anyString()))
                .thenReturn(0);
            when(productRepository.findById(productId))
                .thenReturn(Optional.empty());

            //Act and Assert
            assertThatThrownBy(() -> productService.changeProductStock(productId, request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage(String.format("Product with id %s does not exist", productId));

            verify(securityUtils).getAuthenticatedUsername();
            verify(productRepository).changeProductStock(anyLong(), anyInt(), any(Instant.class), anyString());
            verify(productRepository).findById(productId);
        }

        @Test
        @DisplayName("Should throw InvalidStockModificationException when the product stock was not modified")
        void shouldThrowInvalidStockModificationException_whenProductStockNotModified() {
            //Arrange
            Product product = ProductTestDataFactory.existingDefaultProduct();

            when(securityUtils.getAuthenticatedUsername())
                .thenReturn(TestDataConstants.USER_USERNAME);
            when(productRepository.changeProductStock(anyLong(), anyInt(), any(Instant.class), anyString()))
                .thenReturn(0);
            when(productRepository.findById(productId))
                .thenReturn(Optional.of(product));

            //Act and Assert
            assertThatThrownBy(() -> productService.changeProductStock(productId, request))
                .isInstanceOf(InvalidStockModificationException.class)
                .satisfies(e -> {
                    InvalidStockModificationException exception = (InvalidStockModificationException) e;

                    assertThat(exception.getMessage()).isEqualTo("Invalid stock modification");
                    assertThat(exception.getDetails()).containsExactlyInAnyOrder(
                        String.format("Tried modifying stock by %s for %s existing", request.stockChange(), product.getStock())
                    );
                });

            verify(securityUtils).getAuthenticatedUsername();
            verify(productRepository).changeProductStock(anyLong(), anyInt(), any(Instant.class), anyString());
            verify(productRepository).findById(productId);
        }

        @Test
        @DisplayName("Should modify product stock when product stock change is valid")
        void shouldModifyProductStock_whenProductStockIsValid() {
            //Arrange
            Product product = ProductTestDataFactory.existingDefaultProduct();
            ProductResponse expectedResponse = ProductTestDataFactory.DEFAULT_RESPONSE;

            when(securityUtils.getAuthenticatedUsername())
                .thenReturn(TestDataConstants.USER_USERNAME);
            when(productRepository.changeProductStock(anyLong(), anyInt(), any(Instant.class), anyString()))
                .thenReturn(1);
            when(productRepository.findById(productId))
                .thenReturn(Optional.of(product));
            when(productMapper.toResponse(product))
                .thenReturn(expectedResponse);

            //Act
            ProductResponse response = productService.changeProductStock(productId, request);

            //Assert
            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(expectedResponse.id());
            assertThat(response.stock()).isEqualTo(expectedResponse.stock());

            verify(securityUtils).getAuthenticatedUsername();
            verify(productRepository).changeProductStock(anyLong(), anyInt(), any(Instant.class), anyString());
            verify(productRepository).findById(productId);
            verify(productMapper).toResponse(product);
        }

    }

    @Nested
    @DisplayName("getProduct tests")
    class GetProductTests {

        private final long productId = 1L;

        @Test
        @DisplayName("Should throw EntityNotFoundException when product does not exist by id")
        void shouldThrowEntityNotFoundException_whenProductDoesNotExist() {
            //Arrange
            when(productRepository.findById(productId))
                .thenReturn(Optional.empty());

            //Act and Assert
            assertThatThrownBy(() -> productService.getProduct(productId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage(String.format("Product with id %s does not exist", productId));

            verify(productRepository).findById(productId);
        }

        @Test
        @DisplayName("Should get product when product exists by id")
        void shouldGetProduct_whenProductExists() {
            //Arrange
            Product product = ProductTestDataFactory.existingDefaultProduct();
            ProductResponse expectedResponse = ProductTestDataFactory.DEFAULT_RESPONSE;

            when(productRepository.findById(productId))
                .thenReturn(Optional.of(product));
            when(productMapper.toResponse(product))
                .thenReturn(expectedResponse);

            //Act
            ProductResponse response = productService.getProduct(productId);

            //Assert
            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(expectedResponse.id());
            assertThat(response.name()).isEqualTo(expectedResponse.name());

            verify(productRepository).findById(productId);
            verify(productMapper).toResponse(product);
        }

    }

    @Nested
    @DisplayName("getProducts tests")
    class GetProductsTests {

        @Test
        @DisplayName("Should get a page of products with provided pagination and filtering")
        void shouldGetProductPageWithPaginationAndFiltering() {
            //Arrange
            Pageable pagination = PageRequest.of(0, 10);
            ProductFiltering filtering = new ProductFiltering("Product", null, null, null);
            Specification<Product> spec = Specification.allOf();
            Page<Product> page = new PageImpl<>(List.of(), pagination, 0);
            PaginatedResponse<ProductShortResponse> expectedResponse = ProductTestDataFactory.DEFAULT_PAGINATED_RESPONSE;

            when(specificationBuilder.buildSpecificationFromFiltering(filtering))
                .thenReturn(spec);
            when(productRepository.findAll(spec, pagination))
                .thenReturn(page);
            when(productMapper.toPaginatedResponse(page))
                .thenReturn(expectedResponse);

            //Act
            PaginatedResponse<ProductShortResponse> response = productService.getProducts(pagination, filtering);

            //Assert
            assertThat(response).isNotNull();
            assertThat(response.page()).isEqualTo(expectedResponse.page());
            assertThat(response.pageSize()).isEqualTo(expectedResponse.pageSize());
            assertThat(response.content()).hasSize(expectedResponse.content().size());

            verify(specificationBuilder).buildSpecificationFromFiltering(filtering);
            verify(productRepository).findAll(spec, pagination);
            verify(productMapper).toPaginatedResponse(page);
        }

    }

    @Nested
    @DisplayName("deleteProduct tests")
    class DeleteProductTests{

        @Test
        @DisplayName("Should perform product deletion with provided id")
        void shouldPerformProductDeletionById(){
            //Arrange
            long productId = 1L;

            doNothing()
                .when(productRepository).deleteProductById(productId);

            //Act
            productService.deleteProduct(productId);

            //Assert
            verify(productRepository).deleteProductById(productId);
        }

    }

}
