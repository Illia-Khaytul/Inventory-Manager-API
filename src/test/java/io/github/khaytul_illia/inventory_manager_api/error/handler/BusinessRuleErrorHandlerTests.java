package io.github.khaytul_illia.inventory_manager_api.error.handler;

import io.github.khaytul_illia.inventory_manager_api.support.context.ErrorHandlerTestContext;
import io.github.khaytul_illia.inventory_manager_api.error.exception.InvalidPasswordException;
import io.github.khaytul_illia.inventory_manager_api.error.exception.InvalidStockModificationException;
import io.github.khaytul_illia.inventory_manager_api.error.exception.UserSessionLimitExceededException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;

import static io.github.khaytul_illia.inventory_manager_api.support.utils.mvc.ErrorResponseAssertionUtils.*;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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
        ResultActions result = mockMvc
            .perform(
                get("/dummy")
            )
            .andExpect(status().isForbidden());

        assertRegularErrorResponse(result, HttpStatus.FORBIDDEN, exception.getMessage());
    }

    @Test
    @DisplayName("Should return 400 Bad Request when caught InvalidPasswordException")
    void shouldReturn400_whenInvalidPasswordException() throws Exception {
        //Arrange
        InvalidPasswordException exception = new InvalidPasswordException("message", List.of("error1", "error2"));

        doThrow(exception)
            .when(dummyController).dummyOperation();

        //Act and Assert
        ResultActions result = mockMvc
            .perform(
                get("/dummy")
            )
            .andExpect(status().isBadRequest());

        assertErrorResponse(result, HttpStatus.BAD_REQUEST, exception.getMessage());
        result.andExpect(jsonPath("$.data.errors").value(containsInAnyOrder(exception.getErrorMessages().toArray(new String[]{}))));
    }

    @Test
    @DisplayName("Should return 409 Conflict when caught InvalidStockModificationException")
    void shouldReturn409_whenInvalidStockModificationException() throws Exception {
        //Arrange
        InvalidStockModificationException exception = new InvalidStockModificationException("message", "detail1", "detail2");

        doThrow(exception)
            .when(dummyController).dummyOperation();

        //Act and Assert
        ResultActions result = mockMvc
            .perform(
                get("/dummy")
            )
            .andExpect(status().isConflict());

        assertErrorResponse(result, HttpStatus.CONFLICT, exception.getMessage());
        result.andExpect(jsonPath("$.data.details").value(containsInAnyOrder(exception.getDetails().toArray(new String[]{}))));
    }

}
