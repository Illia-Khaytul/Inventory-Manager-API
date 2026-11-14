package io.github.khaytul_illia.inventory_manager_api.product;

import io.github.khaytul_illia.inventory_manager_api.support.context.RepositoryTestContext;
import io.github.khaytul_illia.inventory_manager_api.support.data.ProductTestDataFactory;
import io.github.khaytul_illia.inventory_manager_api.support.data.TestDataConstants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@DisplayName("ProductRepository tests")
public class ProductRepositoryTests extends RepositoryTestContext {

    @Autowired
    private ProductRepository productRepository;

    @Nested
    @DisplayName("existsByNameAndDeletedAtNull tests")
    class ExistsByNameAndDeletedAtNullTests{

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
            boolean exists = productRepository.existsByNameAndDeletedAtNull(target.getName());

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
            boolean exists = productRepository.existsByNameAndDeletedAtNull(target.getName());

            //Assert
            assertThat(exists).isFalse();
        }

        /*
                Test data provider methods
         */

        static Stream<Arguments> provideProducts(){
            return Stream.of(
                Arguments.of(
                    ProductTestDataFactory.newDefaultProduct(),
                    List.of(
                        ProductTestDataFactory.newUniqueDeletedProduct(),
                        ProductTestDataFactory.newUniqueProduct()
                    )
                )
            );
        }

    }

    @Nested
    @DisplayName("findByIdAndDeletedAtNull tests")
    class FindByIdAndDeletedAtNullTests {

        @ParameterizedTest
        @MethodSource("provideProducts")
        @DisplayName("Should return product when found by id and not deleted")
        void shouldReturnProduct_whenFoundByIdAndNotDeleted(Product target, List<Product> otherProducts){
            //Arrange
            otherProducts.forEach(otherUser -> entityManager.persist(otherUser));
            entityManager.persist(target);
            entityManager.flush();
            entityManager.clear();

            //Act
            Optional<Product> foundProduct = productRepository.findByIdAndDeletedAtNull(target.getId());

            //Assert
            assertThat(foundProduct).isNotEmpty();

            Product product = foundProduct.get();
            assertThat(product.getId()).isEqualTo(target.getId());
            assertThat(product.getName()).isEqualTo(target.getName());
            assertThat(product.getDeletedAt()).isNull();
        }

        @ParameterizedTest
        @MethodSource("provideProducts")
        @DisplayName("Should return empty when product does not found or deleted")
        void shouldReturnEmpty_whenNotFoundOrDeleted(Product target, List<Product> otherProducts){
            //Arrange
            target.setDeletedAt(Instant.now());

            entityManager.persistAndFlush(target);
            otherProducts.forEach(otherUser -> entityManager.persist(otherUser));
            entityManager.flush();
            entityManager.clear();

            //Act
            Optional<Product> foundProduct = productRepository.findByIdAndDeletedAtNull(target.getId());

            //Assert
            assertThat(foundProduct).isEmpty();
        }

        /*
                Test data provider methods
         */

        static Stream<Arguments> provideProducts(){
            return Stream.of(
                Arguments.of(
                    ProductTestDataFactory.newDefaultProduct(),
                    List.of(
                        ProductTestDataFactory.newUniqueDeletedProduct(),
                        ProductTestDataFactory.newUniqueProduct()
                    )
                )
            );
        }

    }

    @Nested
    @DisplayName("changeProductStock tests")
    class ChangeProductStockTests{

        private final Instant modifiedAt = Instant.now();
        private final String modifiedBy = TestDataConstants.USER_USERNAME;

        @Test
        @DisplayName("Should do nothing when product does not exist by id")
        void shouldDoNothing_whenProductDoesNotExist(){
            //Arrange
            Product target = ProductTestDataFactory.newDefaultProduct();
            int stockChange = 10;

            entityManager.persistAndFlush(target);
            entityManager.remove(target);
            entityManager.flush();
            entityManager.clear();

            //Act
            int updatedRows = productRepository.changeProductStock(target.getId(), stockChange, modifiedAt, modifiedBy);

            //Assert
            assertThat(updatedRows).isEqualTo(0);
        }

        @Test
        @DisplayName("Should not modify stock when the changed stock is negative")
        void shouldNotModifyStock_whenChangedStockIsNegative(){
            //Arrange
            Product target = ProductTestDataFactory.newDefaultProduct();
            int stockChange = -100;

            entityManager.persistAndFlush(target);
            entityManager.clear();

            //Act
            int updatedRows = productRepository.changeProductStock(target.getId(), stockChange, modifiedAt, modifiedBy);

            //Assert
            assertThat(updatedRows).isEqualTo(0);
        }

        @Test
        @DisplayName("Should not modify stock when the product is deleted")
        void shouldNotModifyStock_whenProductDeleted(){
            //Arrange
            Product target = ProductTestDataFactory.newDefaultDeletedProduct();
            int stockChange = 10;

            entityManager.persistAndFlush(target);
            entityManager.clear();

            //Act
            int updatedRows = productRepository.changeProductStock(target.getId(), stockChange, modifiedAt, modifiedBy);

            //Assert
            assertThat(updatedRows).isEqualTo(0);
        }

        @ParameterizedTest
        @ValueSource(ints = {-10, 1, 10})
        @DisplayName("Should modify stock when the changed stock is valid")
        void shouldModifyStock_whenChangedStockIsValid(int stockChange){
            //Arrange
            int originalStock = 10;
            Product target = ProductTestDataFactory.defaults().stock(originalStock).build();

            entityManager.persistAndFlush(target);
            entityManager.clear();

            int version = target.getVersion();

            //Act
            int updatedRows = productRepository.changeProductStock(target.getId(), stockChange, modifiedAt, modifiedBy);

            //Assert
            assertThat(updatedRows).isEqualTo(1);

            target = productRepository.findById(target.getId()).orElseThrow();
            assertThat(target.getStock()).isEqualTo(stockChange + originalStock);
            assertThat(target.getModifiedAt()).isCloseTo(modifiedAt, within(10, ChronoUnit.SECONDS));
            assertThat(target.getModifiedBy()).isEqualTo(modifiedBy);
            assertThat(target.getVersion()).isEqualTo(version + 1);
        }

    }

    @Nested
    @DisplayName("deleteProductById tests")
    class DeleteProductByIdTests{

        private final String deletedBy = TestDataConstants.USER_USERNAME;

        @ParameterizedTest
        @MethodSource("provideProducts")
        @DisplayName("Should delete product when product exists by id and is not deleted")
        void shouldDeleteProduct_whenExistsByIdAndNotDeleted(Product target, List<Product> otherProducts){
            //Arrange
            otherProducts.forEach(otherUser -> entityManager.persist(otherUser));
            entityManager.persist(target);
            entityManager.flush();
            entityManager.clear();
            
            //Act
            productRepository.deleteProductById(target.getId(), deletedBy);
            
            //Assert
            Product product = productRepository.findById(target.getId()).orElseThrow();
            assertThat(product.getDeletedAt()).isCloseTo(Instant.now(), within(10, ChronoUnit.SECONDS));
            assertThat(product.getDeletedBy()).isEqualTo(deletedBy);
            assertThat(product.getVersion()).isEqualTo(target.getVersion() + 1);

            otherProducts.forEach(otherProduct -> {
                Product otherFoundProduct = productRepository.findById(otherProduct.getId()).orElseThrow();
                assertThat(otherFoundProduct.getDeletedAt()).satisfiesAnyOf(
                    value -> assertThat(value).isEqualTo(otherProduct.getDeletedAt()),
                    value -> assertThat(value).isCloseTo(otherProduct.getDeletedAt(), within(1, ChronoUnit.SECONDS))
                );
                assertThat(otherFoundProduct.getDeletedBy()).isEqualTo(otherProduct.getDeletedBy());
                assertThat(otherFoundProduct.getVersion()).isEqualTo(otherProduct.getVersion());
            });
        }

        @ParameterizedTest
        @MethodSource("provideProducts")
        @DisplayName("Should do nothing when product does not exist by id")
        void shouldDoNothing_whenDoesNotExistById(Product target, List<Product> otherProducts){
            //Arrange
            entityManager.persistAndFlush(target);
            otherProducts.forEach(otherUser -> entityManager.persist(otherUser));
            entityManager.remove(target);
            entityManager.flush();
            entityManager.clear();
            
            //Act
            productRepository.deleteProductById(target.getId(), deletedBy);
            
            //Assert
            otherProducts.forEach(otherProduct -> {
                Product otherFoundProduct = productRepository.findById(otherProduct.getId()).orElseThrow();
                assertThat(otherFoundProduct.getDeletedAt()).satisfiesAnyOf(
                    value -> assertThat(value).isEqualTo(otherProduct.getDeletedAt()),
                    value -> assertThat(value).isCloseTo(otherProduct.getDeletedAt(), within(1, ChronoUnit.SECONDS))
                );
                assertThat(otherFoundProduct.getDeletedBy()).isEqualTo(otherProduct.getDeletedBy());
                assertThat(otherFoundProduct.getVersion()).isEqualTo(otherProduct.getVersion());
            });
        }

        @ParameterizedTest
        @MethodSource("provideProducts")
        @DisplayName("Should do nothing when product is deleted")
        void shouldDoNothing_whenProductDeleted(Product target, List<Product> otherProducts){
            //Arrange
            target.setDeletedAt(Instant.now());
            target.setDeletedBy(TestDataConstants.USER_USERNAME);

            entityManager.persistAndFlush(target);
            otherProducts.forEach(otherUser -> entityManager.persist(otherUser));
            entityManager.flush();
            entityManager.clear();

            //Act
            productRepository.deleteProductById(target.getId(), deletedBy);

            //Assert
            Product product = productRepository.findById(target.getId()).orElseThrow();
            assertThat(product.getDeletedAt()).isCloseTo(target.getDeletedAt(), within(1, ChronoUnit.SECONDS));
            assertThat(product.getDeletedBy()).isEqualTo(target.getDeletedBy());
            assertThat(product.getVersion()).isEqualTo(target.getVersion());

            otherProducts.forEach(otherProduct -> {
                Product otherFoundProduct = productRepository.findById(otherProduct.getId()).orElseThrow();
                assertThat(otherFoundProduct.getDeletedAt()).satisfiesAnyOf(
                    value -> assertThat(value).isEqualTo(otherProduct.getDeletedAt()),
                    value -> assertThat(value).isCloseTo(otherProduct.getDeletedAt(), within(1, ChronoUnit.SECONDS))
                );
                assertThat(otherFoundProduct.getDeletedBy()).isEqualTo(otherProduct.getDeletedBy());
                assertThat(otherFoundProduct.getVersion()).isEqualTo(otherProduct.getVersion());
            });
        }

        /*
                Test data provider methods
         */

        static Stream<Arguments> provideProducts(){
            return Stream.of(
                Arguments.of(
                    ProductTestDataFactory.newDefaultProduct(),
                    List.of(
                        ProductTestDataFactory.newUniqueDeletedProduct(),
                        ProductTestDataFactory.newUniqueProduct()
                    )
                )
            );
        }

    }

}
