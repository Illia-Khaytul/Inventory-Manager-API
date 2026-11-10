package io.github.khaytul_illia.inventory_manager_api.common.validation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RangePropertyValidator tests")
public class ValidationPropertiesTests {

    private static final int refreshTokenMaxLength = 36;
    private static final int productNameMaxLength = 100;
    private static final int productDescriptionMaxLength = 1000;
    private static final String productPriceMaxValue = String.valueOf(Integer.MAX_VALUE);
    private static final int userPasswordMaxLength = 50;
    private static final int userUsernameMaxLength = 50;
    private final ValidationProperties validationProperties = new ValidationProperties(
        refreshTokenMaxLength,
        productNameMaxLength,
        productDescriptionMaxLength,
        productPriceMaxValue,
        userPasswordMaxLength,
        userUsernameMaxLength
    );

    @Nested
    @DisplayName("getValueLimits tests")
    class GetValueLimitsTests{

        @ParameterizedTest
        @EnumSource(
            value = ValidationProperties.PropertyKey.class,
            names = "PRODUCT_PRICE",
            mode = EnumSource.Mode.INCLUDE
        )
        @DisplayName("Should return no properties when properties do no exist for that key")
        void shouldReturnNoProperties_whenPropertiesDoNotExist(ValidationProperties.PropertyKey key){
            //Act
            Optional<ValidationProperties.ValueLimits> limits = validationProperties.getValueLimits(key);

            //Assert
            assertThat(limits).isEmpty();
        }

        @ParameterizedTest
        @MethodSource("provideValueLimits")
        @DisplayName("Should return property when property exists for that key")
        void shouldReturnProperty_whenPropertyExists(ValidationProperties.PropertyKey key, ValidationProperties.ValueLimits expected){
            //Act
            Optional<ValidationProperties.ValueLimits> limits = validationProperties.getValueLimits(key);

            //Asserts
            assertThat(limits).isNotEmpty();
            assertThat(limits.get().min()).isEqualTo(expected.min());
            assertThat(limits.get().max()).isEqualTo(expected.max());
        }

        /*
                Test data provider methods
         */

        static Stream<Arguments> provideValueLimits(){
            return Stream.of(
                Arguments.of(
                    ValidationProperties.PropertyKey.REFRESH_TOKEN,
                    new ValidationProperties.ValueLimits(0, refreshTokenMaxLength)
                ),
                Arguments.of(
                    ValidationProperties.PropertyKey.PRODUCT_NAME,
                    new ValidationProperties.ValueLimits(0, productNameMaxLength)
                ),
                Arguments.of(
                    ValidationProperties.PropertyKey.PRODUCT_DESCRIPTION,
                    new ValidationProperties.ValueLimits(0, productDescriptionMaxLength)
                ),
                Arguments.of(
                    ValidationProperties.PropertyKey.USER_USERNAME,
                    new ValidationProperties.ValueLimits(0, userUsernameMaxLength)
                ),
                Arguments.of(
                    ValidationProperties.PropertyKey.USER_PASSWORD,
                    new ValidationProperties.ValueLimits(0, userPasswordMaxLength)
                )
            );
        }

    }

    @Nested
    @DisplayName("getNumericLimits tests")
    class GetNumericLimitsTests{

        @ParameterizedTest
        @EnumSource(
            value = ValidationProperties.PropertyKey.class,
            names = "PRODUCT_PRICE",
            mode = EnumSource.Mode.EXCLUDE
        )
        @DisplayName("Should return no properties when properties do no exist for that key")
        void shouldReturnNoProperties_whenPropertiesDoNotExist(ValidationProperties.PropertyKey key){
            //Act
            Optional<ValidationProperties.NumericLimits> limits = validationProperties.getNumericLimits(key);

            //Assert
            assertThat(limits).isEmpty();
        }

        @ParameterizedTest
        @MethodSource("provideNumericLimits")
        @DisplayName("Should return property when property exists for that key")
        void shouldReturnProperty_whenPropertyExists(ValidationProperties.PropertyKey key, ValidationProperties.NumericLimits expected){
            //Act
            Optional<ValidationProperties.NumericLimits> limits = validationProperties.getNumericLimits(key);

            //Asserts
            assertThat(limits).isNotEmpty();
            assertThat(limits.get().min()).isEqualTo(expected.min());
            assertThat(limits.get().max()).isEqualTo(expected.max());
        }

        /*
                Test data provider methods
         */

        static Stream<Arguments> provideNumericLimits(){
            return Stream.of(
                Arguments.of(
                    ValidationProperties.PropertyKey.PRODUCT_PRICE,
                    new ValidationProperties.NumericLimits("0", productPriceMaxValue)
                )
            );
        }

    }

}
