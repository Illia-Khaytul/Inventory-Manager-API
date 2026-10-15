package io.github.khaytul_illia.inventory_manager_api.error.handler;

import io.github.khaytul_illia.inventory_manager_api.error.ErrorResponse;
import io.github.khaytul_illia.inventory_manager_api.error.exception.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalErrorHandler {

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse genericExceptionHandler(Exception e){
        log.warn("[EXCEPTION] Caught unexpected exception", e);

        return new ErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Something went wrong"
        );
    }

}
