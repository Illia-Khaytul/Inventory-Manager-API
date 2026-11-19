package io.github.khaytul_illia.inventory_manager_api.order.order.response;

import com.fasterxml.jackson.annotation.JsonView;
import io.github.khaytul_illia.inventory_manager_api.common.response.view.ResponseViews;
import io.github.khaytul_illia.inventory_manager_api.order.item.response.OrderItemResponse;
import io.github.khaytul_illia.inventory_manager_api.order.order.Order;
import io.github.khaytul_illia.inventory_manager_api.user.response.UserResponse;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(
    Long id,
    Order.OrderStatus status,
    BigDecimal totalPrice,
    @JsonView(ResponseViews.OperatorView.class)
    UserResponse customer,
    List<OrderItemResponse> items,
    Instant createdAt,
    String createdBy,
    Instant modifiedAt,
    String modifiedBy
) {
}
