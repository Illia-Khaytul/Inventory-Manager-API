package io.github.khaytul_illia.inventory_manager_api.common.monetary;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MonetaryJsonDeserializer tests")
public class MonetaryJsonDeserializerTests {

    private JsonParser jsonParser;
    private DeserializationContext context;
    private final MonetaryJsonDeserializer deserializer = new MonetaryJsonDeserializer();

    @BeforeEach
    void beforeEach(){
        jsonParser = mock(JsonParser.class);
        context = mock(DeserializationContext.class);
    }

    @Test
    @DisplayName("Should return null when json value is null")
    void shouldReturnNull_whenJsonValueNull(){
        //Arrange
        when(jsonParser.hasToken(JsonToken.VALUE_NULL))
            .thenReturn(true);

        //Act
        BigDecimal value = deserializer.deserialize(jsonParser, context);

        //Assert
        assertThat(value).isNull();

        verify(jsonParser).hasToken(JsonToken.VALUE_NULL);
    }

    @Test
    @DisplayName("Should return big decimal when json value is not null")
    void shouldReturnBigDecimal_whenJsonValueNotNull(){
        //Arrange
        MockedStatic<Decimals> decimalsMock = mockStatic(Decimals.class);
        BigDecimal decimalValue = new BigDecimal("99.99");

        when(jsonParser.hasToken(JsonToken.VALUE_NULL))
            .thenReturn(false);
        when(jsonParser.getDecimalValue())
            .thenReturn(decimalValue);
        decimalsMock.when(() -> Decimals.of(decimalValue))
            .thenReturn(decimalValue);

        //Act
        BigDecimal value = deserializer.deserialize(jsonParser, context);

        //Assert
        assertThat(value).isNotNull();
        assertThat(value).isEqualTo(decimalValue);

        verify(jsonParser).hasToken(JsonToken.VALUE_NULL);
        verify(jsonParser).getDecimalValue();
        decimalsMock.verify(() -> Decimals.of(decimalValue));

        decimalsMock.close();
    }

}
