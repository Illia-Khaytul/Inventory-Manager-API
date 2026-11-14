package io.github.khaytul_illia.inventory_manager_api.product;

import io.github.khaytul_illia.inventory_manager_api.product.request.ProductFiltering;
import io.github.khaytul_illia.inventory_manager_api.security.SecurityUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductSpecificationBuilder tests")
public class ProductSpecificationBuilderTests {

    @Mock
    private SecurityUtils securityUtils;
    @InjectMocks
    private ProductSpecificationBuilder specificationBuilder;

    @Nested
    @DisplayName("scopeFiltersByUserPermission tests")
    class ScopeFiltersByUserPermissionTests{

        @ParameterizedTest
        @MethodSource("provideProductFiltering")
        @DisplayName("Should return the same filtering when authenticated user is an OPERATOR")
        void shouldReturnSameFiltering_whenUserIsOperator(ProductFiltering filtering){
            //Arrange
            when(securityUtils.isAuthenticatedUserOperator())
                .thenReturn(true);

            //Act
            ProductFiltering modified = specificationBuilder.scopeFiltersByUserPermission(filtering);

            //Assert
            assertThat(modified).isNotNull();
            assertThat(modified.nameContains()).isEqualTo(filtering.nameContains());
            assertThat(modified.minStock()).isEqualTo(filtering.minStock());
            assertThat(modified.minPrice()).isEqualTo(filtering.minPrice());
            assertThat(modified.maxPrice()).isEqualTo(filtering.maxPrice());
            assertThat(modified.showDeleted()).isEqualTo(filtering.showDeleted());

            verify(securityUtils).isAuthenticatedUserOperator();
        }

        @ParameterizedTest
        @MethodSource("provideProductFiltering")
        @DisplayName("Should return the same filtering when authenticated user is an OPERATOR")
        void shouldReturnModifiedFiltering_whenUserIsNotOperator(ProductFiltering filtering){
            //Arrange
            when(securityUtils.isAuthenticatedUserOperator())
                .thenReturn(false);

            //Act
            ProductFiltering modified = specificationBuilder.scopeFiltersByUserPermission(filtering);

            //Assert
            assertThat(modified).isNotNull();
            assertThat(modified.nameContains()).isEqualTo(filtering.nameContains());
            assertThat(modified.minStock()).isEqualTo(filtering.minStock());
            assertThat(modified.minPrice()).isEqualTo(filtering.minPrice());
            assertThat(modified.maxPrice()).isEqualTo(filtering.maxPrice());
            assertThat(modified.showDeleted()).isFalse();

            verify(securityUtils).isAuthenticatedUserOperator();
        }

        /*
                Test data provider methods
         */

        static Stream<Arguments> provideProductFiltering(){
            return Stream.of(
                Arguments.of(
                    new ProductFiltering("product", null, null, new BigDecimal("99.99"), false)
                ),
                Arguments.of(
                    new ProductFiltering("product", 10, new BigDecimal("20"), new BigDecimal("99.99"), true)
                ),
                Arguments.of(
                    new ProductFiltering(null, 5, null, null, true)
                )
            );
        }

    }

}
