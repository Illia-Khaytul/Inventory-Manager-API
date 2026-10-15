package io.github.khaytul_illia.inventory_manager_api.error.handler;

import io.github.khaytul_illia.inventory_manager_api.error.ErrorResponse;
import io.github.khaytul_illia.inventory_manager_api.error.exception.FailedLoginAuthenticationException;
import io.github.khaytul_illia.inventory_manager_api.error.exception.InvalidRefreshTokenException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class SecurityErrorHandler {

    @ExceptionHandler(FailedLoginAuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorResponse failedLoginAuthenticationHandler(FailedLoginAuthenticationException e){
        log.info("Caught {}: {} - With cause {}: {}", e.getClass().getName(), e.getMessage(), e.getCause().getClass().getName(), e.getCause().getMessage());

        return new ErrorResponse(
            HttpStatus.UNAUTHORIZED,
            e.getMessage()
        );
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorResponse invalidRefreshTokenHandler(InvalidRefreshTokenException e){
        log.info("Caught {}: {} - {}", e.getClass().getName(), e.getMessage(), e.getDetails());

        return new ErrorResponse(
            HttpStatus.UNAUTHORIZED,
            e.getMessage()
        );
    }

}
