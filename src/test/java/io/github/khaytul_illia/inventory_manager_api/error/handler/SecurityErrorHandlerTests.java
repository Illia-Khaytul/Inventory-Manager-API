package io.github.khaytul_illia.inventory_manager_api.error.handler;

import io.github.khaytul_illia.inventory_manager_api.support.context.ErrorHandlerTestContext;
import io.github.khaytul_illia.inventory_manager_api.error.exception.FailedLoginAuthenticationException;
import io.github.khaytul_illia.inventory_manager_api.error.exception.InvalidRefreshTokenException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;

import static io.github.khaytul_illia.inventory_manager_api.support.utils.mvc.ErrorResponseAssertionUtils.assertRegularErrorResponse;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("SecurityErrorHandler tests")
public class SecurityErrorHandlerTests extends ErrorHandlerTestContext {

    @Test
    @DisplayName("Should return 401 Unauthorized when caught FailedLoginAuthenticationException")
    void shouldReturn401_whenFailedLoginAuthenticationException() throws Exception {
        //Arrange
        FailedLoginAuthenticationException exception = new FailedLoginAuthenticationException(new BadCredentialsException("message"));

        doThrow(exception)
            .when(dummyController).dummyOperation();

        //Act and Assert
        ResultActions result = mockMvc
            .perform(
                get("/dummy")
            )
            .andExpect(status().isUnauthorized());

        assertRegularErrorResponse(result, HttpStatus.UNAUTHORIZED, exception.getMessage());
    }

    @Test
    @DisplayName("Should return 401 Unauthorized when caught InvalidRefreshTokenException")
    void shouldReturn401_whenInvalidRefreshTokenException() throws Exception {
        //Arrange
        InvalidRefreshTokenException exception = new InvalidRefreshTokenException("message", List.of());

        doThrow(exception)
            .when(dummyController).dummyOperation();

        //Act and Assert
        ResultActions result = mockMvc
            .perform(
                get("/dummy")
            )
            .andExpect(status().isUnauthorized());

        assertRegularErrorResponse(result, HttpStatus.UNAUTHORIZED, exception.getMessage());
    }

}
