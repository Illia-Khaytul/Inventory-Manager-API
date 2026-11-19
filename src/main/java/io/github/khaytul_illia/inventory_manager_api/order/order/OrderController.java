package io.github.khaytul_illia.inventory_manager_api.order.order;

import io.github.khaytul_illia.inventory_manager_api.common.response.pagination.PaginatedResponse;
import io.github.khaytul_illia.inventory_manager_api.common.response.view.RoleBasedJsonView;
import io.github.khaytul_illia.inventory_manager_api.order.order.request.OrderFiltering;
import io.github.khaytul_illia.inventory_manager_api.order.order.response.OrderResponse;
import io.github.khaytul_illia.inventory_manager_api.order.order.response.OrderShortResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = "/orders")
@Tag(
    name = "Orders",
    description = "API for order management and viewing"
)
@SecurityRequirement(name = "JWT authentication")
public class OrderController {

    @PostMapping(path = "")
    @RoleBasedJsonView
    public ResponseEntity<OrderResponse> createOrder(){
        return null;
    }

    @GetMapping(path = "/{orderId}")
    @ResponseStatus(HttpStatus.OK)
    @RoleBasedJsonView
    public OrderResponse getOrder(
        @PathVariable @Positive long orderId
    ){
        return null;
    }

    @GetMapping(path = "")
    @ResponseStatus(HttpStatus.OK)
    @RoleBasedJsonView
    public PaginatedResponse<OrderShortResponse> getOrders(
        @PageableDefault Pageable pagination,
        @ModelAttribute @Valid OrderFiltering filtering
    ){
        return null;
    }

    @DeleteMapping(path = "/{orderId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteOrder(
        @PathVariable @Positive long orderId
    ){

    }

}
