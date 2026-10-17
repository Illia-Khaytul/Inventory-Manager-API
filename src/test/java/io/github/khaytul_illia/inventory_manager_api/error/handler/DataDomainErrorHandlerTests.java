package io.github.khaytul_illia.inventory_manager_api.error.handler;

import io.github.khaytul_illia.inventory_manager_api.support.context.ErrorHandlerTestContext;
import io.github.khaytul_illia.inventory_manager_api.error.ErrorResponse;
import io.github.khaytul_illia.inventory_manager_api.error.exception.DuplicateEntryException;
import io.github.khaytul_illia.inventory_manager_api.error.exception.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.web.servlet.MvcResult;

import java.io.UnsupportedEncodingException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("DataDomainErrorHandler tests")
public class DataDomainErrorHandlerTests extends ErrorHandlerTestContext {

    @Test
    @DisplayName("Should return 409 Conflict when caught DuplicateEntryException")
    void shouldReturn409_whenDuplicateEntryException() throws Exception {
        //Arrange
        DuplicateEntryException exception = new DuplicateEntryException("message");

        doThrow(exception)
            .when(dummyController).dummyOperation();

        //Act and Assert
        mockMvc
            .perform(
                get("/dummy")
            )
            .andExpect(status().isConflict())
            .andExpect(result -> {
                assertRegularErrorResponse(result, HttpStatus.CONFLICT, exception.getMessage());
            });
    }

    @Test
    @DisplayName("Should return 404 Not Found when caught EntityNotFoundException")
    void shouldReturn404_whenEntityNotFoundException() throws Exception {
        //Arrange
        EntityNotFoundException exception = new EntityNotFoundException("message");

        doThrow(exception)
            .when(dummyController).dummyOperation();

        //Act and Assert
        mockMvc
            .perform(
                get("/dummy")
            )
            .andExpect(status().isNotFound())
            .andExpect(result -> {
                assertRegularErrorResponse(result, HttpStatus.NOT_FOUND, exception.getMessage());
            });
    }

    @ParameterizedTest
    @MethodSource("provideOptimisticLockingExceptions")
    @DisplayName("Should return 409 Conflict when caught OptimisticLockingFailureException")
    void shouldReturn409_whenOptimisticLockingFailureException(OptimisticLockingFailureException exception) throws Exception {
        doThrow(exception)
            .when(dummyController).dummyOperation();

        //Act and Assert
        mockMvc
            .perform(
                get("/dummy")
            )
            .andExpect(status().isConflict())
            .andExpect(result -> {
                assertRegularErrorResponse(result, HttpStatus.CONFLICT, "Concurrent modification error");
            });
    }

    /*
            Test data provider methods
     */

    static Stream<Arguments> provideOptimisticLockingExceptions(){
        return Stream.of(
            //General optimistic locking exception
            Arguments.of(
                new OptimisticLockingFailureException("message")
            ),
            //Object optimistic locking exception
            Arguments.of(
                new ObjectOptimisticLockingFailureException("message", new RuntimeException())
            )
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
