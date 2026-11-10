package io.github.khaytul_illia.inventory_manager_api.support.utils.mvc;

import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

public class ErrorResponseAssertionUtils {

    public static void assertErrorResponse(ResultActions result, HttpStatus status, String message) throws Exception {
        result
            .andExpect(jsonPath("$.timestamp").isNotEmpty())
            .andExpect(jsonPath("$.status").value(status.value()))
            .andExpect(jsonPath("$.message").value(message));
    }

    public static void assertRegularErrorResponse(ResultActions result, HttpStatus status, String message) throws Exception {
        assertErrorResponse(result, status, message);

        result.andExpect(jsonPath("$.data").isEmpty());
    }

    public static void assertValidationErrorResponse(ResultActions result, Map<String, List<String>> expectedErrors) throws Exception {
        assertErrorResponse(result, HttpStatus.BAD_REQUEST, "Invalid request parameters");

        result.andExpect(jsonPath("$.data", aMapWithSize(expectedErrors.size())));
        for(Map.Entry<String, List<String>> error: expectedErrors.entrySet()){
            result.andExpect(jsonPath("$.data." + error.getKey()).value(containsInAnyOrder(error.getValue().toArray(new String[]{}))));
        }
    }

    public static void assertSingleErrorValidationErrorResponse(ResultActions result, Map<String, String> expectedErrors) throws Exception {
        assertValidationErrorResponse(result, expectedErrors.entrySet()
            .stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> List.of(entry.getValue())
            ))
        );
    }

    public static void assertNoAuthErrorResponse(ResultActions result) throws Exception {
        assertRegularErrorResponse(result, HttpStatus.UNAUTHORIZED, "Authentication required to access this resource");
    }

    public static void assertForbiddenErrorResponse(ResultActions result) throws Exception {
        assertRegularErrorResponse(result, HttpStatus.FORBIDDEN, "Forbidden from accessing this resource");
    }

}
