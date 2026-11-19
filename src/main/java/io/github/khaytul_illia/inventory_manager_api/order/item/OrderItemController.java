package io.github.khaytul_illia.inventory_manager_api.order.item;

import io.github.khaytul_illia.inventory_manager_api.order.item.request.ModifyQuantityRequest;
import io.github.khaytul_illia.inventory_manager_api.order.item.request.OrderItemCreateRequest;
import io.github.khaytul_illia.inventory_manager_api.order.item.response.OrderItemChangeResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = "/orders/{orderId}/items")
@Tag(
    name = "Order Items",
    description = "API for order item management"
)
@SecurityRequirement(name = "JWT authentication")
public class OrderItemController {

    @PostMapping(path = "")
    @ResponseStatus(HttpStatus.OK)
    public OrderItemChangeResponse addItem(
        @PathVariable @Positive long orderId,
        @RequestBody @Valid OrderItemCreateRequest request
    ){
        return null;
    }

    @PatchMapping(path = "/{productId}")
    @ResponseStatus(HttpStatus.OK)
    public OrderItemChangeResponse changeItemQuantity(
        @PathVariable @Positive long orderId,
        @PathVariable @Positive long productId,
        @RequestBody @Valid ModifyQuantityRequest request
    ){
        return null;
    }

    @DeleteMapping(path = "/{productId}")
    @ResponseStatus(HttpStatus.OK)
    public OrderItemChangeResponse createOrder(
        @PathVariable @Positive long orderId,
        @PathVariable @Positive long productId
    ){
        return null;
    }

    @PostMapping(path = "")
    @ResponseStatus(HttpStatus.OK)
    public OrderItemChangeResponse createOrder(
        @PathVariable @Positive long orderId
    ){
        return null;
    }

}
