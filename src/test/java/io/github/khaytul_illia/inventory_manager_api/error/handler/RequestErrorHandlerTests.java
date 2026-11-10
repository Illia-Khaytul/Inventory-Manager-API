package io.github.khaytul_illia.inventory_manager_api.error.handler;

import io.github.khaytul_illia.inventory_manager_api.support.DummyController;
import io.github.khaytul_illia.inventory_manager_api.support.context.ErrorHandlerTestContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static io.github.khaytul_illia.inventory_manager_api.support.utils.mvc.ErrorResponseAssertionUtils.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("RequestErrorHandler tests")
public class RequestErrorHandlerTests extends ErrorHandlerTestContext {

    @ParameterizedTest
    @MethodSource("provideInvalidIdValues")
    @DisplayName("Should return 400 Bad Request when receiving invalid request parameters")
    void shouldReturn400_whenInvalidRequestParameters(
        long id,
        String query,
        Map<String, List<String>> expectedErrors
    ) throws Exception {
        //Act and Assert
        ResultActions result = mockMvc
            .perform(
                patch("/dummy/{id}", id)
                    .param("query", query)
            )
            .andExpect(status().isBadRequest());

        assertValidationErrorResponse(result, expectedErrors);
    }

    @ParameterizedTest
    @MethodSource("provideInvalidDummyRequests")
    @DisplayName("Should return 400 Bad Request when receiving invalid request body parameters")
    void shouldReturn400_whenInvalidRequestBody(
        DummyController.DummyRequest invalidRequest,
        Map<String, List<String>> expectedErrors
    ) throws Exception {
        //Act and Assert
        ResultActions result = mockMvc
            .perform(
                post("/dummy")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidRequest))
            )
            .andExpect(status().isBadRequest());

        assertValidationErrorResponse(result, expectedErrors);
    }

    @Test
    @DisplayName("Should return 400 Bad Request when receiving wrong request parameter type")
    void shouldReturn400_whenWrongRequestType() throws Exception {
        //Arrange
        String value = "not a long";

        //Act and Assert
        ResultActions result = mockMvc
            .perform(
                patch("/dummy/{id}", value)
            )
            .andExpect(status().isBadRequest());

        assertErrorResponse(result, HttpStatus.BAD_REQUEST, "Invalid request parameter type");
        result
            .andExpect(jsonPath("$.data.parameter").value("id"))
            .andExpect(jsonPath("$.data.received_value").value(value))
            .andExpect(jsonPath("$.data.required_type").value("long"));
    }

    @Test
    @DisplayName("Should return 400 Bad Request when receiving an unreadable request body")
    void shouldReturn400_whenUnreadableRequestBody() throws Exception{
        //Act and Assert
        ResultActions result = mockMvc
            .perform(
                post("/dummy")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("not a json")
            )
            .andExpect(status().isBadRequest());

        assertRegularErrorResponse(result, HttpStatus.BAD_REQUEST, "Malformed request body");
    }

    @Test
    @DisplayName("Should return 415 Unsupported Media Type when receiving request body of an unsupported media type")
    void shouldReturn415_whenUnsupportedRequestBodyType() throws Exception {
        //Act and Assert
        ResultActions result = mockMvc
            .perform(
                post("/dummy")
                    .contentType(MediaType.TEXT_PLAIN)
                    .content("plain text")
            )
            .andExpect(status().isUnsupportedMediaType());

        assertErrorResponse(result, HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Unsupported request media type");
        result
            .andExpect(jsonPath("$.data.received").value("text"))
            .andExpect(jsonPath("$.data.supported").value(hasItems(MediaType.APPLICATION_JSON_VALUE)));
    }

    @Test
    @DisplayName("Should return 404 Not Found when accessing resource does not exist")
    void shouldReturn404_whenResourceNotFound() throws Exception{
        //Act and Assert
        ResultActions result = mockMvc
            .perform(
                get("/non-existent-resource")
            )
            .andExpect(status().isNotFound());

        assertRegularErrorResponse(result, HttpStatus.NOT_FOUND, "Resource not found");
    }

    /*
            Test data provider methods
     */

    static Stream<Arguments> provideInvalidIdValues(){
        return Stream.of(
            //Single violation on one field
            Arguments.of(
                1, "valid",
                Map.of("id", List.of("must be greater than or equal to 2"))
            ),
            //Single violation on each field
            Arguments.of(
                1, "",
                Map.of("id", List.of("must be greater than or equal to 2"), "query", List.of("must not be empty"))
            ),
            //Multiple violations on one field
            Arguments.of(
                -1, "",
                Map.of("id", List.of("must be greater than 0", "must be greater than or equal to 2"), "query", List.of("must not be empty"))
            )
        );
    }

    static Stream<Arguments> provideInvalidDummyRequests() {
        return Stream.of(
            //Single violation on one field
            Arguments.of(
                new DummyController.DummyRequest(null, "valid"),
                Map.of("value1", List.of("must not be null"))
            ),
            //Single violation on each field
            Arguments.of(
                new DummyController.DummyRequest(null, "too long"),
                Map.of("value1", List.of("must not be null"), "value2", List.of("size must be between 2 and 5"))
            ),
            //Multiple violations on one field
            Arguments.of(
                new DummyController.DummyRequest(null, ""),
                Map.of("value1", List.of("must not be null"), "value2", List.of("must not be empty", "size must be between 2 and 5"))
            )
        );
    }

}
