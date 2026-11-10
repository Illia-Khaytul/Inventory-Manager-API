package io.github.khaytul_illia.inventory_manager_api.common.monetary;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;

import java.math.BigDecimal;

import static org.mockito.Mockito.*;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("MonetaryJsonSerializer tests")
public class MonetaryJsonSerializerTests {

    private JsonGenerator jsonGenerator;
    private SerializationContext context;
    private final MonetaryJsonSerializer serializer = new MonetaryJsonSerializer();

    @BeforeEach
    void beforeEach(){
        jsonGenerator = mock(JsonGenerator.class);
        context = mock(SerializationContext.class);
    }

    @Test
    @DisplayName("Should write null when value is null")
    void shouldWriteNull_whenValueNull(){
        //Arrange
        when(jsonGenerator.writeNull())
            .thenReturn(jsonGenerator);

        //Act
        serializer.serialize(null, jsonGenerator, context);

        //Assert
        verify(jsonGenerator).writeNull();
    }

    @Test
    @DisplayName("Should write value as numeric when value is not null")
    void shouldWriteNumericValue_whenValueNotNull(){
        //Arrange
        MockedStatic<Decimals> decimalsMock = mockStatic(Decimals.class);
        BigDecimal value = new BigDecimal("99.99");

        decimalsMock.when(() -> Decimals.of(value))
            .thenReturn(value);
        when(jsonGenerator.writeNumber(value))
            .thenReturn(jsonGenerator);

        //Act
        serializer.serialize(value, jsonGenerator, context);

        //Assert
        decimalsMock.verify(() -> Decimals.of(value));
        verify(jsonGenerator).writeNumber(value);

        decimalsMock.close();
    }

}
