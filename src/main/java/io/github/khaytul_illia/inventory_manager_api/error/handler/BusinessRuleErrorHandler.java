package io.github.khaytul_illia.inventory_manager_api.error.handler;

import io.github.khaytul_illia.inventory_manager_api.error.ErrorResponse;
import io.github.khaytul_illia.inventory_manager_api.error.exception.InvalidPasswordException;
import io.github.khaytul_illia.inventory_manager_api.error.exception.InvalidStockModificationException;
import io.github.khaytul_illia.inventory_manager_api.error.exception.UserSessionLimitExceededException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class BusinessRuleErrorHandler {

    @ExceptionHandler(UserSessionLimitExceededException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ErrorResponse userSessionLimitExceededHandler(UserSessionLimitExceededException e){
        log.info("Caught {}: {}", e.getClass().getName(), e.getMessage());

        return new ErrorResponse(
            HttpStatus.FORBIDDEN,
            e.getMessage()
        );
    }

    @ExceptionHandler(InvalidPasswordException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse invalidPasswordHandler(InvalidPasswordException e){
        Map<String, Object> data = new HashMap<>();
        data.put("errors", e.getErrorMessages());

        log.debug("Caught {}: {} - {}", e.getClass().getName(), e.getMessage(), data);

        return new ErrorResponse(
            HttpStatus.BAD_REQUEST,
            e.getMessage(),
            data
        );
    }

    @ExceptionHandler(InvalidStockModificationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse invalidStockModificationHandler(InvalidStockModificationException e){
        Map<String, Object> data = new HashMap<>();
        data.put("details", e.getDetails());

        log.debug("Caught {}: {} - {}", e.getClass().getName(), e.getMessage(), data);

        return new ErrorResponse(
            HttpStatus.CONFLICT,
            e.getMessage(),
            data
        );
    }

}
