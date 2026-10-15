package io.github.khaytul_illia.inventory_manager_api.error.handler;

import io.github.khaytul_illia.inventory_manager_api.DummyController;
import io.github.khaytul_illia.inventory_manager_api.error.ErrorResponse;
import io.github.khaytul_illia.inventory_manager_api.error.exception.FailedLoginAuthenticationException;
import io.github.khaytul_illia.inventory_manager_api.error.exception.InvalidRefreshTokenException;
import io.github.khaytul_illia.inventory_manager_api.security.SecurityUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

import java.io.UnsupportedEncodingException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DummyController.class)
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = "logging.level.io.github.khaytul_illia.inventory_manager_api=DEBUG")
@ActiveProfiles("test")
@DisplayName("SecurityErrorHandler tests")
public class SecurityErrorHandlerTests {

    @MockitoSpyBean
    private DummyController dummyController;
    @MockitoBean
    private SecurityUtils securityUtils;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Should return 401 Unauthorized when caught FailedLoginAuthenticationException")
    void shouldReturn401_whenFailedLoginAuthenticationException() throws Exception {
        //Arrange
        FailedLoginAuthenticationException exception = new FailedLoginAuthenticationException(new BadCredentialsException("message"));

        doThrow(exception)
            .when(dummyController).dummyOperation();

        //Act and Assert
        mockMvc
            .perform(
                get("/dummy")
            )
            .andExpect(status().isUnauthorized())
            .andExpect(result -> {
                assertRegularErrorResponse(result, HttpStatus.UNAUTHORIZED, exception.getMessage());
            });
    }

    @Test
    @DisplayName("Should return 401 Unauthorized when caught InvalidRefreshTokenException")
    void shouldReturn401_whenInvalidRefreshTokenException() throws Exception {
        //Arrange
        InvalidRefreshTokenException exception = new InvalidRefreshTokenException("message", List.of());

        doThrow(exception)
            .when(dummyController).dummyOperation();

        //Act and Assert
        mockMvc
            .perform(
                get("/dummy")
            )
            .andExpect(status().isUnauthorized())
            .andExpect(result -> {
                assertRegularErrorResponse(result, HttpStatus.UNAUTHORIZED, exception.getMessage());
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

    private ErrorResponse deserializeErrorResponse(MvcResult result) throws UnsupportedEncodingException {
        return objectMapper.readValue(result.getResponse().getContentAsString(), ErrorResponse.class);
    }

}
