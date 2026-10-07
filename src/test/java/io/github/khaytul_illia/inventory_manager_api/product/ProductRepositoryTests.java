package io.github.khaytul_illia.inventory_manager_api.product;

import io.github.khaytul_illia.inventory_manager_api.TestcontainersConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@DisplayName("ProductRepository tests")
public class ProductRepositoryTests {

    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private TestEntityManager entityManager;

    @Nested
    @DisplayName("existsByName tests")
    class ExistsByNameTests{

        @ParameterizedTest
        @MethodSource("provideProducts")
        @DisplayName("Should return true when product exists by name")
        void shouldReturnTrue_whenExistsByName(Product target, List<Product> otherProducts){
            //Arrange
            otherProducts.forEach(otherUser -> entityManager.persist(otherUser));
            entityManager.persist(target);
            entityManager.flush();
            entityManager.clear();

            //Act
            boolean exists = productRepository.existsByName(target.getName());

            //Assert
            assertThat(exists).isTrue();
        }

        @ParameterizedTest
        @MethodSource("provideProducts")
        @DisplayName("Should return false when product does not exist by name")
        void shouldReturnFalse_whenDoesNotExistByName(Product target, List<Product> otherProducts){
            //Arrange
            otherProducts.forEach(otherUser -> entityManager.persist(otherUser));
            entityManager.flush();
            entityManager.clear();

            //Act
            boolean exists = productRepository.existsByName(target.getName());

            //Assert
            assertThat(exists).isFalse();
        }

        /*
                Test data provider methods
         */

        static Stream<Arguments> provideProducts(){
            Instant createdAt = Instant.now();
            String createdBy = "operator";

            return Stream.of(
                //Target session alone
                Arguments.of(
                    new Product(null, "product_name", null, 0, BigDecimal.ONE, createdAt, createdBy, null, null, null),
                    List.of()
                ),
                //Target and other sessions
                Arguments.of(
                    new Product(null, "product_name", null, 0, BigDecimal.ONE, createdAt, createdBy, null, null, null),
                    List.of(
                        new Product(null, "other1", null, 0, BigDecimal.ONE, createdAt, createdBy, null, null, null),
                        new Product(null, "other2", null, 0, BigDecimal.ONE, createdAt, createdBy, null, null, null)
                    )
                )
            );
        }

    }

}
