package io.github.khaytul_illia.inventory_manager_api.order.order.request;

import io.github.khaytul_illia.inventory_manager_api.common.validation.NullOrNotBlank;
import io.github.khaytul_illia.inventory_manager_api.common.validation.RangeProperty;
import io.github.khaytul_illia.inventory_manager_api.common.validation.SizeProperty;
import io.github.khaytul_illia.inventory_manager_api.common.validation.ValidationProperties;
import io.github.khaytul_illia.inventory_manager_api.order.order.Order;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.beans.ConstructorProperties;
import java.math.BigDecimal;
import java.util.Set;

public record OrderFiltering(
    
    @RangeProperty(key = ValidationProperties.PropertyKey.PRICE_RANGE)
    BigDecimal minTotalPrice,

    @RangeProperty(key = ValidationProperties.PropertyKey.PRICE_RANGE)
    BigDecimal maxTotalPrice,

    @PositiveOrZero
    Integer minItems,

    @PositiveOrZero
    Integer maxItems,
    
    Set<Order.OrderStatus> orderStatus,

    @Positive
    Long ownerId,

    @NullOrNotBlank
    @SizeProperty(key = ValidationProperties.PropertyKey.USER_USERNAME)
    String ownerUsernameContains
    
) {

    @ConstructorProperties({"min_total_price", "max_total_price", "min_items", "max_items", "order_status", "owner_id", "owner_username_contains"})
    public OrderFiltering(
        BigDecimal minTotalPrice,
        BigDecimal maxTotalPrice,
        Integer minItems,
        Integer maxItems,
        Set<Order.OrderStatus> orderStatus,
        Long ownerId,
        String ownerUsernameContains
    ) {
        this.minTotalPrice = minTotalPrice;
        this.maxTotalPrice = maxTotalPrice;
        this.minItems = minItems;
        this.maxItems = maxItems;
        this.orderStatus = orderStatus;
        this.ownerId = ownerId;
        this.ownerUsernameContains = ownerUsernameContains;
    }

    public boolean isEmpty(){
        return minTotalPrice == null && maxTotalPrice == null && minItems == null && maxItems == null
            && (orderStatus == null || orderStatus.isEmpty()) && ownerId == null && ownerUsernameContains == null;
    }

}
