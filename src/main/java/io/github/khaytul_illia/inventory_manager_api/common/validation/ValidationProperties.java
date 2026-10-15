package io.github.khaytul_illia.inventory_manager_api.common.validation;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

@Component
public class ValidationProperties {

    public static final ValueLimits DEFAULT_VALUE_LIMITS = new ValueLimits(0, Integer.MAX_VALUE);
    public static final NumericLimits DEFAULT_NUMERIC_LIMITS = new NumericLimits(BigDecimal.ZERO, new BigDecimal(Integer.MAX_VALUE));

    private final Map<PropertyKey, ValueLimits> valueLimitsProperties;
    private final Map<PropertyKey, NumericLimits> numericLimitsProperties;

    public ValidationProperties(
        @Value("${app.auth.refresh-token.max-length}")
        int refreshTokenMaxLength,
        @Value("${app.product.name.max-length}")
        int productNameMaxLength,
        @Value("${app.product.description.max-length}")
        int productDescriptionMaxLength,
        @Value("${app.product.price.max-value}")
        String productPriceMaxValue,
        @Value("${app.user.password.max-length}")
        int userPasswordMaxLength,
        @Value("${app.user.username.max-length}")
        int userUsernameMaxLength
    ) {
        valueLimitsProperties = Map.of(
            PropertyKey.REFRESH_TOKEN, new ValueLimits(0, refreshTokenMaxLength),
            PropertyKey.PRODUCT_NAME, new ValueLimits(0, productNameMaxLength),
            PropertyKey.PRODUCT_DESCRIPTION, new ValueLimits(0, productDescriptionMaxLength),
            PropertyKey.USER_PASSWORD, new ValueLimits(0, userPasswordMaxLength),
            PropertyKey.USER_USERNAME, new ValueLimits(0, userUsernameMaxLength)
        );
        numericLimitsProperties = Map.of(
            PropertyKey.PRODUCT_PRICE, new NumericLimits("0", productPriceMaxValue)
        );
    }

    public Optional<ValueLimits> getValueLimits(PropertyKey propertyKey){
        return Optional.ofNullable(valueLimitsProperties.get(propertyKey));
    }

    public Optional<NumericLimits> getNumericLimits(PropertyKey propertyKey){
        return Optional.ofNullable(numericLimitsProperties.get(propertyKey));
    }

    public enum PropertyKey{
        REFRESH_TOKEN,
        PRODUCT_NAME,
        PRODUCT_DESCRIPTION,
        PRODUCT_PRICE,
        USER_USERNAME,
        USER_PASSWORD
    }
    
    public record ValueLimits(
        int min,
        int max
    ){}
    
    public record NumericLimits(
        BigDecimal min,
        BigDecimal max
    ){

        public NumericLimits(String min, String max){
            this(new BigDecimal(min), new BigDecimal(max));
        }

    }

}
