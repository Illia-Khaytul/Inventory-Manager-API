package io.github.khaytul_illia.inventory_manager_api.product;

import io.github.khaytul_illia.inventory_manager_api.product.request.CreateProductRequest;
import io.github.khaytul_illia.inventory_manager_api.product.response.ProductResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.time.Instant;
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
    void shouldCreateProductFromCreateProductRequest(String name, String description, BigDecimal price){
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
    @MethodSource("provideProductInstances")
    @DisplayName("Should map product entity to product response")
    void shouldMapProductToProductResponse(Product product){
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

    /*
            Test data provider methods
     */

    static Stream<Arguments> provideProductInstances(){
        Instant createdAt = Instant.now();
        Instant modifiedAt = Instant.parse("2026-10-05T18:55:10Z");
        BigDecimal price = new BigDecimal("20.55");

        return Stream.of(
            //Full product entity
            Arguments.of(
                new Product(1L, "product name", "description", 10, price, createdAt, "operator1", modifiedAt, "operator2", 1)
            ),
            //Null description and modified at/by fields
            Arguments.of(
                new Product(2L, "different product", null, 1, price, createdAt, "operator1", null, null, 2)
            )
        );
    }

}
