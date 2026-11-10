package io.github.khaytul_illia.inventory_manager_api.error.handler;

import io.github.khaytul_illia.inventory_manager_api.support.context.ErrorHandlerTestContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.ResultActions;

import java.util.stream.Stream;

import static io.github.khaytul_illia.inventory_manager_api.support.utils.mvc.ErrorResponseAssertionUtils.assertRegularErrorResponse;
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
        ResultActions result = mockMvc
            .perform(
                get("/dummy")
            )
            .andExpect(status().isInternalServerError());

        assertRegularErrorResponse(result, HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong");
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

}
