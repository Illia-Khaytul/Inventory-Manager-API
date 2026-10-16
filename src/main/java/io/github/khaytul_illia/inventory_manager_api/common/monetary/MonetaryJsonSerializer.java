package io.github.khaytul_illia.inventory_manager_api.common.monetary;

import org.springframework.boot.jackson.JacksonComponent;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

import java.math.BigDecimal;

@JacksonComponent
public class MonetaryJsonSerializer extends ValueSerializer<BigDecimal> {

    @Override
    public void serialize(BigDecimal value, JsonGenerator gen, SerializationContext context) throws JacksonException {
        if(value == null){
            gen.writeNull();

            return;
        }

        gen.writeNumber(Decimals.of(value));
    }

}
