package io.github.khaytul_illia.inventory_manager_api.error.handler;

import io.github.khaytul_illia.inventory_manager_api.support.context.ErrorHandlerTestContext;
import io.github.khaytul_illia.inventory_manager_api.error.ErrorResponse;
import io.github.khaytul_illia.inventory_manager_api.error.exception.InvalidPasswordException;
import io.github.khaytul_illia.inventory_manager_api.error.exception.InvalidStockModificationException;
import io.github.khaytul_illia.inventory_manager_api.error.exception.UserSessionLimitExceededException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MvcResult;

import java.io.UnsupportedEncodingException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("BusinessRuleErrorHandler tests")
public class BusinessRuleErrorHandlerTests extends ErrorHandlerTestContext {

    @Test
    @DisplayName("Should return 403 Forbidden when caught UserSessionLimitExceededException")
    void shouldReturn403_whenUserSessionLimitExceededException() throws Exception {
        //Arrange
        UserSessionLimitExceededException exception = new UserSessionLimitExceededException("message");

        doThrow(exception)
            .when(dummyController).dummyOperation();

        //Act and Assert
        mockMvc
            .perform(
                get("/dummy")
            )
            .andExpect(status().isForbidden())
            .andExpect(result -> {
                assertRegularErrorResponse(result, HttpStatus.FORBIDDEN, exception.getMessage());
            });
    }

    @Test
    @DisplayName("Should return 400 Bad Request when caught InvalidPasswordException")
    void shouldReturn400_whenInvalidPasswordException() throws Exception {
        //Arrange
        InvalidPasswordException exception = new InvalidPasswordException("message", List.of("error1", "error2"));
        Map<String, List<String>> data = Map.of("errors", exception.getErrorMessages());

        doThrow(exception)
            .when(dummyController).dummyOperation();

        //Act and Assert
        mockMvc
            .perform(
                get("/dummy")
            )
            .andExpect(status().isBadRequest())
            .andExpect(result -> {
                assertValidationErrorResponse(result, exception.getMessage(), data);
            });
    }

    @Test
    @DisplayName("Should return 409 Conflict when caught InvalidStockModificationException")
    void shouldReturn409_whenInvalidStockModificationException() throws Exception {
        //Arrange
        InvalidStockModificationException exception = new InvalidStockModificationException("message", "detail1", "detail2");

        doThrow(exception)
            .when(dummyController).dummyOperation();

        //Act and Assert
        mockMvc
            .perform(
                get("/dummy")
            )
            .andExpect(status().isConflict())
            .andExpect(result -> {
                ErrorResponse response = deserializeErrorResponse(result);
                Map<String, Object> data = response.data();

                assertErrorResponse(response, HttpStatus.CONFLICT, exception.getMessage());
                //noinspection unchecked
                assertThat((List<String>) data.get("details")).containsExactlyInAnyOrderElementsOf(exception.getDetails());
            });
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

    private void assertValidationErrorResponse(MvcResult result, String message, Map<String, List<String>> expectedErrors) throws UnsupportedEncodingException {
        ErrorResponse response = deserializeErrorResponse(result);

        assertErrorResponse(response, HttpStatus.BAD_REQUEST, message);
        assertThat(response.data().keySet()).containsExactlyInAnyOrderElementsOf(expectedErrors.keySet());
        //noinspection unchecked
        response.data().forEach((field, errors) -> assertThat((List<String>) errors).containsExactlyInAnyOrderElementsOf(expectedErrors.get(field)));
    }

    private ErrorResponse deserializeErrorResponse(MvcResult result) throws UnsupportedEncodingException {
        return objectMapper.readValue(result.getResponse().getContentAsString(), ErrorResponse.class);
    }

}
