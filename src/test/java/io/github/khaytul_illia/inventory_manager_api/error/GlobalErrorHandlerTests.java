package io.github.khaytul_illia.inventory_manager_api.error;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

import java.io.UnsupportedEncodingException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DummyController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("GlobalErrorHandler tests")
public class GlobalErrorHandlerTests {

    @MockitoSpyBean
    private DummyController dummyController;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @ParameterizedTest
    @MethodSource("provideInvalidIdValue")
    @DisplayName("Should return 400 Bad Request when receiving invalid request parameters")
    void shouldReturn400_whenInvalidRequestParameters(
        long id,
        String query,
        Map<String, List<String>> expectedErrors
    ) throws Exception {
        //Act and Assert
        mockMvc
            .perform(
                patch("/dummy/{id}", id)
                    .param("query", query)
            )
            .andExpect(status().isBadRequest())
            .andExpect(result -> assertValidationErrorResponse(result, "Invalid request parameters", expectedErrors));
    }

    @ParameterizedTest
    @MethodSource("provideInvalidDummyRequests")
    @DisplayName("Should return 400 Bad Request when receiving invalid request body parameters")
    void shouldReturn400_whenInvalidRequestBody(
        DummyController.DummyRequest invalidRequest,
        Map<String, List<String>> expectedErrors
    ) throws Exception {
        //Act and Assert
        mockMvc
            .perform(
                post("/dummy")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidRequest))
            )
            .andExpect(status().isBadRequest())
            .andExpect(result -> assertValidationErrorResponse(result, "Invalid request parameters", expectedErrors));
    }

    @Test
    @DisplayName("Should return 400 Bad Request when receiving wrong request parameter type")
    void shouldReturn400_whenWrongRequestType() throws Exception {
        //Arrange
        String value = "not a long";

        //Act and Assert
        mockMvc
            .perform(
                patch("/dummy/{id}", value)
            )
            .andExpect(status().isBadRequest())
            .andExpect(result -> {
                ErrorResponse response = deserializeErrorResponse(result);
                Map<String, Object> data = response.data();

                assertErrorResponse(response, HttpStatus.BAD_REQUEST, "Invalid request parameter type");
                assertThat(data.get("parameter")).isEqualTo("id");
                assertThat(data.get("receivedValue")).isEqualTo(value);
                assertThat(data.get("requiredType")).isEqualTo("long");
            });
    }

    @Test
    @DisplayName("Should return 400 Bad Request when receiving an unreadable request body")
    void shouldReturn400_whenUnreadableRequestBody() throws Exception{
        //Act and Assert
        mockMvc
            .perform(
                post("/dummy")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("not a json")
            )
            .andExpect(status().isBadRequest())
            .andExpect(result -> assertRegularErrorResponse(result, HttpStatus.BAD_REQUEST, "Malformed request body"));
    }

    @Test
    @DisplayName("Should return 415 Unsupported Media Type when receiving request body of an unsupported media type")
    void shouldReturn415_whenUnsupportedRequestBodyType() throws Exception {
        //Act and Assert
        mockMvc
            .perform(
                post("/dummy")
                    .contentType(MediaType.TEXT_PLAIN)
                    .content("plain text")
            )
            .andExpect(status().isUnsupportedMediaType())
            .andExpect(result -> {
                ErrorResponse response = deserializeErrorResponse(result);
                Map<String, Object> data = response.data();

                assertErrorResponse(response, HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Unsupported request media type");
                assertThat(data.get("received")).isEqualTo("text");
                //noinspection unchecked
                assertThat((List<String>) data.get("supported")).contains(MediaType.APPLICATION_JSON_VALUE);
            });
    }

    @Test
    @DisplayName("Should return 404 Not Found when accessing resource does not exist")
    void shouldReturn404_whenResourceNotFound() throws Exception{
        //Act and Assert
        mockMvc
            .perform(
                get("/non-existent-resource")
            )
            .andExpect(status().isNotFound())
            .andExpect(result -> assertRegularErrorResponse(result, HttpStatus.NOT_FOUND, "Resource not found"));
    }

    @Test
    @DisplayName("Should return 500 Internal Server Error when an unexpected exception is thrown")
    void shouldReturn500_whenUnexpectedException() throws Exception{
        //Arrange
        doThrow(new RuntimeException())
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

    static Stream<Arguments> provideInvalidIdValue(){
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
