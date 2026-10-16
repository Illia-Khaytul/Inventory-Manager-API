package io.github.khaytul_illia.inventory_manager_api.common.response.serialization;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

public class SnakeCaseSerializer extends ValueSerializer<String> {

    @Override
    public void serialize(String value, JsonGenerator gen, SerializationContext context) throws JacksonException {
        if(value != null && !value.isBlank()){
            value = value.replaceAll("([a-z])([A-Z]+)", "$1_$2").toLowerCase();
        }

        gen.writeName(value);
    }

}
