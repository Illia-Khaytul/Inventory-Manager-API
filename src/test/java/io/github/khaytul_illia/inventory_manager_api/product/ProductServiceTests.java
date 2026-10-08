package io.github.khaytul_illia.inventory_manager_api.product;

import io.github.khaytul_illia.inventory_manager_api.error.exception.DuplicateEntryException;
import io.github.khaytul_illia.inventory_manager_api.error.exception.EntityNotFoundException;
import io.github.khaytul_illia.inventory_manager_api.error.exception.InvalidStockModificationException;
import io.github.khaytul_illia.inventory_manager_api.product.request.CreateProductRequest;
import io.github.khaytul_illia.inventory_manager_api.product.request.ModifyStockRequest;
import io.github.khaytul_illia.inventory_manager_api.product.request.UpdateProductRequest;
import io.github.khaytul_illia.inventory_manager_api.product.response.ProductResponse;
import io.github.khaytul_illia.inventory_manager_api.security.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

import java.math.BigDecimal;
import java.time.Instant;
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
    private ProductMapper productMapper;
    @Mock
    private SecurityUtils securityUtils;
    @InjectMocks
    private ProductService productService;

    @Nested
    @DisplayName("createProduct tests")
    class CreateProductTests{

        private final CreateProductRequest request = new CreateProductRequest("product_name", null, BigDecimal.ONE);

        @Test
        @DisplayName("Should throw DuplicateEntryException when product name is not unique")
        void shouldThrowDuplicateEntryException_whenProductNameIsNotUnique(){
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
        void shouldCreateNewProduct_whenProductNameIsUnique(){
            //Arrange
            Product product = new Product();
            product.setId(1L);
            product.setName(request.name());
            ProductResponse expectedResponse = new ProductResponse(product.getId(), product.getName(), null, 0, null, null, null, null, null);

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
        private final UpdateProductRequest request = new UpdateProductRequest("product_name", null, BigDecimal.ONE);

        @Test
        @DisplayName("Should throw EntityNotFoundException when product does not exist by id")
        void shouldThrowEntityNotFoundException_whenProductDoesNotExist(){
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
        void shouldReturnProductResponse_whenRequestIsEmpty(){
            //Arrange
            UpdateProductRequest request = new UpdateProductRequest(null, null, null);
            Product product = new Product();
            ProductResponse productResponse = new ProductResponse(1L, "Original Name", null, 0, new BigDecimal("11.11"), null, null, null, null);

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
        void shouldThrowDuplicateEntryException_whenNewProductNameIsNotUnique(){
            //Arrange
            Product product = new Product();
            product.setName(request.name() + "_different");

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
        void shouldNotThrowDuplicateEntryException_whenNewProductNameIsNullOrEqualToOldName(String oldName, String newName){
            //Arrange
            UpdateProductRequest request = new UpdateProductRequest(newName, "description", null);
            Product product = new Product();
            product.setName(oldName);
            ProductResponse productResponse = new ProductResponse(1L, newName, null, 0, new BigDecimal("11.11"), null, null, null, null);

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
            verify(productRepository).findById(productId);
            verify(productRepository, never()).existsByName(request.name());
            verify(productMapper).updateProduct(product, request);
            verify(productMapper).toResponse(product);
        }

        @Test
        @DisplayName("Should update the found product when the new product name is unique")
        void shouldUpdateProduct_whenNewProductNameIsUnique(){
            //Arrange
            Product product = new Product();
            product.setName(request.name() + "_different");
            ProductResponse productResponse = new ProductResponse(1L, request.name(), null, 0, new BigDecimal("11.11"), null, null, null, null);

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
        private final ModifyStockRequest request = new ModifyStockRequest(10);
        private Jwt jwtMock;

        @BeforeEach
        void beforeEach(){
            String authenticatedUsername = "operator";
            jwtMock = mock(Jwt.class);

            when(jwtMock.getSubject())
                .thenReturn(authenticatedUsername);
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when product does not exist by id")
        void shouldThrowEntityNotFoundException_whenProductDoesNotExist() {
            //Arrange
            when(securityUtils.getAuthenticatedUserAccessToken())
                .thenReturn(jwtMock);
            when(productRepository.changeProductStock(anyLong(), anyInt(), any(Instant.class), anyString()))
                .thenReturn(0);
            when(productRepository.findById(productId))
                .thenReturn(Optional.empty());

            //Act and Assert
            assertThatThrownBy(() -> productService.changeProductStock(productId, request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage(String.format("Product with id %s does not exist", productId));

            verify(securityUtils).getAuthenticatedUserAccessToken();
            verify(productRepository).changeProductStock(anyLong(), anyInt(), any(Instant.class), anyString());
            verify(productRepository).findById(productId);
        }

        @Test
        @DisplayName("Should throw InvalidStockModificationException when the product stock was not modified")
        void shouldThrowInvalidStockModificationException_whenProductStockNotModified() {
            //Arrange
            Product product = new Product(1L, "Original Product", null, 10, new BigDecimal("10.10"), null, null, null, null, null);

            when(securityUtils.getAuthenticatedUserAccessToken())
                .thenReturn(jwtMock);
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

            verify(securityUtils).getAuthenticatedUserAccessToken();
            verify(productRepository).changeProductStock(anyLong(), anyInt(), any(Instant.class), anyString());
            verify(productRepository).findById(productId);
        }

        @Test
        @DisplayName("Should modify product stock when product stock change is valid")
        void shouldModifyProductStock_whenProductStockIsValid() {
            //Arrange
            Product product = new Product(1L, "Original Product", null, 10, new BigDecimal("10.10"), null, null, null, null, null);
            ProductResponse expectedResponse = new ProductResponse(1L, "Original Product", null, 10, new BigDecimal("10.10"), null, null, null, null);

            when(securityUtils.getAuthenticatedUserAccessToken())
                .thenReturn(jwtMock);
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

            verify(securityUtils).getAuthenticatedUserAccessToken();
            verify(productRepository).changeProductStock(anyLong(), anyInt(), any(Instant.class), anyString());
            verify(productRepository).findById(productId);
            verify(productMapper).toResponse(product);
        }

    }
}
