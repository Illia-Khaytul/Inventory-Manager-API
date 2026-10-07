package io.github.khaytul_illia.inventory_manager_api.product;

import io.github.khaytul_illia.inventory_manager_api.error.exception.DuplicateEntryException;
import io.github.khaytul_illia.inventory_manager_api.product.request.CreateProductRequest;
import io.github.khaytul_illia.inventory_manager_api.product.response.ProductResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductService tests")
public class ProductServiceTests {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private ProductMapper productMapper;
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

}
