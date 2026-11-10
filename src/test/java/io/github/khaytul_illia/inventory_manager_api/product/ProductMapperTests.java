package io.github.khaytul_illia.inventory_manager_api.product;

import io.github.khaytul_illia.inventory_manager_api.common.response.pagination.PaginatedResponse;
import io.github.khaytul_illia.inventory_manager_api.product.request.CreateProductRequest;
import io.github.khaytul_illia.inventory_manager_api.product.request.UpdateProductRequest;
import io.github.khaytul_illia.inventory_manager_api.product.response.ProductResponse;
import io.github.khaytul_illia.inventory_manager_api.product.response.ProductShortResponse;
import io.github.khaytul_illia.inventory_manager_api.support.data.ProductTestDataFactory;
import io.github.khaytul_illia.inventory_manager_api.support.data.TestDataConstants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mapstruct.factory.Mappers;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ProductMapper tests")
public class ProductMapperTests {

    private final ProductMapper productMapper = Mappers.getMapper(ProductMapper.class);

    @ParameterizedTest
    @CsvSource(
        nullValues = "NULL",
        quoteCharacter = '"',
        textBlock = """
            product_name, draft_description, "50.5"
            different_name, NULL, "105.69"
            computer, "   ", "999.99"
            """)
    @DisplayName("Should create new product instance from the provided create product request")
    void shouldCreateProductFromCreateProductRequest(String name, String description, BigDecimal price) {
        //Arrange
        CreateProductRequest request = new CreateProductRequest(name, description, price);

        //Act
        Product product = productMapper.buildProduct(request);

        //Assert
        assertThat(product).isNotNull();
        assertThat(product.getId()).isNull();
        assertThat(product.getName()).isEqualTo(name);
        assertThat(product.getDescription()).isEqualTo(description);
        assertThat(product.getStock()).isEqualTo(0);
        assertThat(product.getPrice()).isEqualTo(price);
        assertThat(product.getCreatedAt()).isNull();
        assertThat(product.getCreatedBy()).isNull();
        assertThat(product.getModifiedAt()).isNull();
        assertThat(product.getModifiedBy()).isNull();
        assertThat(product.getVersion()).isNull();
    }

    @ParameterizedTest
    @MethodSource("provideProductUpdateData")
    @DisplayName("Should update the product with the not null update product request values")
    void shouldUpdateProductWithNotNullDataFromUpdateProductRequest(Product actual, Product expected, UpdateProductRequest request) {
        //Act
        productMapper.updateProduct(actual, request);

        //Assert
        assertThat(actual.getName()).isEqualTo(expected.getName());
        assertThat(actual.getDescription()).isEqualTo(expected.getDescription());
        assertThat(actual.getPrice()).isEqualTo(expected.getPrice());
    }

    @ParameterizedTest
    @MethodSource("provideProductInstances")
    @DisplayName("Should map product entity to product response")
    void shouldMapProductToProductResponse(Product product) {
        //Act
        ProductResponse response = productMapper.toResponse(product);

        //Assert
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(product.getId());
        assertThat(response.name()).isEqualTo(product.getName());
        assertThat(response.description()).isEqualTo(product.getDescription());
        assertThat(response.stock()).isEqualTo(product.getStock());
        assertThat(response.price()).isEqualTo(product.getPrice());
        assertThat(response.createdAt()).isEqualTo(product.getCreatedAt());
        assertThat(response.createdBy()).isEqualTo(product.getCreatedBy());
        assertThat(response.modifiedAt()).isEqualTo(product.getModifiedAt());
        assertThat(response.modifiedBy()).isEqualTo(product.getModifiedBy());
    }

    @ParameterizedTest
    @MethodSource("provideProductInstances")
    @DisplayName("Should map product entity to product short response")
    void shouldMapProductToProductShortResponse(Product product) {
        //Act
        ProductShortResponse response = productMapper.toShortResponse(product);

        //Assert
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(product.getId());
        assertThat(response.name()).isEqualTo(product.getName());
        assertThat(response.stock()).isEqualTo(product.getStock());
        assertThat(response.price()).isEqualTo(product.getPrice());
    }

    @ParameterizedTest
    @MethodSource("provideProductPages")
    @DisplayName("Should map product pages to paginated response of product short responses")
    void shouldMapProductPageToPaginatedResponseOfProductShortResponses(Page<Product> page) {
        //Act
        PaginatedResponse<ProductShortResponse> response = productMapper.toPaginatedResponse(page);

        //Assert
        assertThat(response).isNotNull();
        assertThat(response.page()).isEqualTo(page.getNumber());
        assertThat(response.pageSize()).isEqualTo(page.getSize());
        assertThat(response.totalPages()).isEqualTo(page.getTotalPages());
        assertThat(response.totalElements()).isEqualTo(page.getTotalElements());

        List<ProductShortResponse> content = response.content();
        List<Product> products = page.getContent();
        assertThat(content).isNotNull();
        assertThat(content).hasSize(products.size());
        for (int i = 0; i < content.size(); i++) {
            assertThat(content.get(i).id()).isEqualTo(products.get(i).getId());
            assertThat(content.get(i).name()).isEqualTo(products.get(i).getName());
        }
    }

    /*
            Test data provider methods
     */

    static Stream<Arguments> provideProductInstances() {
        return Stream.of(
            //Full product entity
            Arguments.of(
                ProductTestDataFactory.newUniqueProduct(10, "20.25")
            ),
            //Null description and modified at/by fields
            Arguments.of(
                ProductTestDataFactory.defaults().audited().stock(1).price("19.99").build()
            )
        );
    }

    static Stream<Arguments> provideProductUpdateData() {
        String newName = TestDataConstants.PRODUCT_NAME + "_new";
        String newPrice = "1" + TestDataConstants.PRODUCT_PRICE;

        return Stream.of(
            //All fields set
            Arguments.of(
                ProductTestDataFactory.newDefaultProduct(),
                ProductTestDataFactory.newProduct(newName, 0, TestDataConstants.PRODUCT_PRICE),
                new UpdateProductRequest(newName, null, null)
            ),
            //Some fields missing
            Arguments.of(
                ProductTestDataFactory.newDefaultProduct(),
                ProductTestDataFactory.newProduct(newName, 0, newPrice),
                new UpdateProductRequest(newName, null, new BigDecimal(newPrice))
            ),
            //All fields missing
            Arguments.of(
                ProductTestDataFactory.newDefaultProduct(),
                ProductTestDataFactory.newProduct(TestDataConstants.PRODUCT_NAME, 0, TestDataConstants.PRODUCT_PRICE),
                new UpdateProductRequest(null, null, null)
            )
        );
    }

    static Stream<Arguments> provideProductPages() {
        return Stream.of(
            //Empty page
            Arguments.of(
                new PageImpl<>(
                    List.of(),
                    PageRequest.of(0, 10),
                    0
                )
            ),
            //Few products for a large page
            Arguments.of(
                new PageImpl<>(
                    List.of(
                        ProductTestDataFactory.existingUniqueProduct(),
                        ProductTestDataFactory.existingUniqueProduct()
                    ),
                    PageRequest.of(0, 10),
                    2
                )
            ),
            //Many products for a small page
            Arguments.of(
                new PageImpl<>(
                    List.of(
                        ProductTestDataFactory.existingUniqueProduct(),
                        ProductTestDataFactory.existingUniqueProduct(),
                        ProductTestDataFactory.existingUniqueProduct(),
                        ProductTestDataFactory.existingUniqueProduct(),
                        ProductTestDataFactory.existingUniqueProduct()
                    ),
                    PageRequest.of(1, 5),
                    10
                )
            )
        );
    }

}
