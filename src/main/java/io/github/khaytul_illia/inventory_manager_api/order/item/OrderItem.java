package io.github.khaytul_illia.inventory_manager_api.order.item;

import io.github.khaytul_illia.inventory_manager_api.common.monetary.Decimals;
import io.github.khaytul_illia.inventory_manager_api.order.order.Order;
import io.github.khaytul_illia.inventory_manager_api.product.Product;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class OrderItem {

    @Id
    @EmbeddedId
    private OrderItemId id;

    @Transient
    private BigDecimal totalPrice;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private int reserved;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "product_price", nullable = false)
    private BigDecimal productPrice;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("orderId")
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("productId")
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @PostLoad
    public void postLoad(){
        totalPrice = Decimals.multiply(productPrice, quantity);
    }

    public record OrderItemId(

        @Column(name = "order_id", nullable = false)
        long orderId,

        @Column(name = "product_id", nullable = false)
        long productId

    ){}

}
