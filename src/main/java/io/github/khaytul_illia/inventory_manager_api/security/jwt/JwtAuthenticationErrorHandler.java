package io.github.khaytul_illia.inventory_manager_api.security.jwt;

import io.github.khaytul_illia.inventory_manager_api.error.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

@Component
@Slf4j
public class JwtAuthenticationErrorHandler implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public JwtAuthenticationErrorHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(
        HttpServletRequest request,
        HttpServletResponse response,
        AuthenticationException e
    ) throws IOException {
        if(e instanceof AuthenticationServiceException){
            log.warn("[EXCEPTION] Something went wrong during JWT authentication", e);

            handleInternalAuthenticationError(response);

            return;
        }

        log.info("Caught {} - {}", e.getClass().getName(), e.getMessage());

        String message = switch (e) {
            case InvalidBearerTokenException ignored -> "Invalid or expired access token";
            case OAuth2AuthenticationException ignored -> "Access token cannot be resolved";
            default -> "Authentication required to access this resource";
        };

        ErrorResponse errorResponse = new ErrorResponse(HttpStatus.UNAUTHORIZED, message);

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), errorResponse);
    }

    private void handleInternalAuthenticationError(HttpServletResponse response) throws IOException {
        ErrorResponse errorResponse = new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong during authentication");

        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), errorResponse);
    }

}
