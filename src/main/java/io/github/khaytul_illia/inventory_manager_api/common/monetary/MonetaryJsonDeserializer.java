package io.github.khaytul_illia.inventory_manager_api.common.monetary;

import org.springframework.boot.jackson.JacksonComponent;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

import java.math.BigDecimal;

@JacksonComponent
public class MonetaryJsonDeserializer extends ValueDeserializer<BigDecimal> {

    @Override
    public BigDecimal deserialize(JsonParser p, DeserializationContext context) throws JacksonException {
        if(p.hasToken(JsonToken.VALUE_NULL)){
            return null;
        }

        return Decimals.of(p.getDecimalValue());
    }
}
