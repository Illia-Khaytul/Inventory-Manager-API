package io.github.khaytul_illia.inventory_manager_api.product.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.github.khaytul_illia.inventory_manager_api.common.validation.NullOrNotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.Range;

import java.math.BigDecimal;

public record UpdateProductRequest(

    @NullOrNotBlank
    @Size(max = 100)
    String name,

    @NullOrNotBlank
    @Size(max = 1000)
    String description,

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @Range(min = 0, max = Integer.MAX_VALUE)
    BigDecimal price

) {

    public boolean isEmpty(){
        return name == null && description == null && price == null;
    }

}
