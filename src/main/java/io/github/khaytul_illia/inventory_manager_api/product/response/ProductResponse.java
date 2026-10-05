package io.github.khaytul_illia.inventory_manager_api.product.response;

import com.fasterxml.jackson.annotation.JsonView;

import java.math.BigDecimal;
import java.time.Instant;

public record ProductResponse(
    Long id,
    String name,
    String description,
    int stock,
    BigDecimal price,
    @JsonView(OperatorView.class)
    Instant createdAt,
    @JsonView(OperatorView.class)
    String createdBy,
    @JsonView(OperatorView.class)
    Instant modifiedAt,
    @JsonView(OperatorView.class)
    String modifiedBy
) {

    public interface OperatorView{}

}
