package io.github.khaytul_illia.inventory_manager_api.error.handler;

import io.github.khaytul_illia.inventory_manager_api.support.context.ErrorHandlerTestContext;
import io.github.khaytul_illia.inventory_manager_api.error.ErrorResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MvcResult;

import java.io.UnsupportedEncodingException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("GlobalErrorHandler tests")
public class GlobalErrorHandlerTests extends ErrorHandlerTestContext {

    @ParameterizedTest
    @MethodSource("provideUnexpectedExceptions")
    @DisplayName("Should return 500 Internal Server Error when an unexpected exception is thrown")
    void shouldReturn500_whenUnexpectedException(RuntimeException exception) throws Exception{
        //Arrange
        doThrow(exception)
            .when(dummyController).dummyOperation();

        //Act and Assert
        mockMvc
            .perform(
                get("/dummy")
            )
            .andExpect(status().isInternalServerError())
            .andExpect(result -> assertRegularErrorResponse(result, HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong"));
    }

    /*
            Test data provider methods
     */

    static Stream<Arguments> provideUnexpectedExceptions(){
        return Stream.of(
            //Random RuntimeException
            Arguments.of(new RuntimeException("runtime exception message")),
            //IllegalStateException
            Arguments.of(new IllegalStateException("this is not allowed"))
        );
    }

    /*
            Helper methods
     */

    private void assertErrorResponse(ErrorResponse response, HttpStatus status, String message){
        assertThat(response.timestamp()).isCloseTo(Instant.now(), within(1, ChronoUnit.MINUTES));
        assertThat(response.status()).isEqualTo(status.value());
        assertThat(response.message()).isEqualTo(message);
    }

    private void assertRegularErrorResponse(MvcResult result, HttpStatus status, String message) throws UnsupportedEncodingException {
        ErrorResponse response = deserializeErrorResponse(result);

        assertErrorResponse(response, status, message);
        assertThat(response.data()).isEmpty();
    }

    private ErrorResponse deserializeErrorResponse(MvcResult result) throws UnsupportedEncodingException {
        return objectMapper.readValue(result.getResponse().getContentAsString(), ErrorResponse.class);
    }

}
