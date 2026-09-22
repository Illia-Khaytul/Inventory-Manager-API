package io.github.khaytul_illia.inventory_manager_api.error;

import io.github.khaytul_illia.inventory_manager_api.error.exception.UserSessionLimitExceededException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalErrorHandler {

    @ExceptionHandler(UserSessionLimitExceededException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ErrorResponse userSessionLimitExceededHandler(UserSessionLimitExceededException e){
        log.info("Caught {}: {}", e.getClass().getName(), e.getMessage());

        return new ErrorResponse(
            HttpStatus.FORBIDDEN,
            e.getMessage()
        );
    }

    @ExceptionHandler(BadCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorResponse badCredentialsHandler(BadCredentialsException e){
        log.info("Caught {}: {}", e.getClass().getName(), e.getMessage());

        return new ErrorResponse(
            HttpStatus.UNAUTHORIZED,
            e.getMessage()
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse methodArgumentNotValidHandler(MethodArgumentNotValidException e){
        Map<String, Object> errors = new HashMap<>();
        e.getBindingResult().getFieldErrors().forEach(error -> {
            @SuppressWarnings("unchecked")
            List<String> messages = (List<String>) errors.computeIfAbsent(error.getField(), key -> new ArrayList<>());
            messages.add(error.getDefaultMessage());
        });

        log.info("Caught {}: {} - {}", e.getClass().getName(), e.getMessage(), errors);

        return new ErrorResponse(
            HttpStatus.BAD_REQUEST,
            "Invalid request parameters",
            errors
        );
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handlerMethodValidationHandler(HandlerMethodValidationException e){
        Map<String, Object> errors = new HashMap<>();
        e.getParameterValidationResults().forEach(error -> {
            List<String> messages = error.getResolvableErrors().stream()
                .map(MessageSourceResolvable::getDefaultMessage)
                .toList();
            errors.put(error.getMethodParameter().getParameterName(), messages);
        });

        log.info("Caught {}: {} - {}", e.getClass().getName(), e.getMessage(), errors);

        return new ErrorResponse(
            HttpStatus.BAD_REQUEST,
            "Invalid request parameters",
            errors
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse methodArgumentTypeMismatchHandler(MethodArgumentTypeMismatchException e){
        Map<String, Object> data = new HashMap<>();
        data.put("parameter", e.getParameter().getParameterName());
        data.put("receivedValue", e.getValue());
        data.put("requiredType", e.getRequiredType() != null ? e.getRequiredType().getSimpleName() : null);

        log.info("Caught {}: {}", e.getClass().getName(), e.getMessage());

        return new ErrorResponse(
            HttpStatus.BAD_REQUEST,
            "Invalid request parameter type",
            data
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse httpMessageNotReadableHandler(HttpMessageNotReadableException e){
        log.info("Caught {}: {}", e.getClass().getName(), e.getMessage());

        return new ErrorResponse(
            HttpStatus.BAD_REQUEST,
            "Malformed request body"
        );
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    @ResponseStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
    public ErrorResponse httpMediaTypeNotSupportedHandler(HttpMediaTypeNotSupportedException e){
        Map<String, Object> data = new HashMap<>();
        data.put("received", e.getContentType() != null ? e.getContentType().getType() : null);
        data.put("supported", e.getSupportedMediaTypes().stream().map(MediaType::toString).toList());

        log.info("Caught {}: {}", e.getClass().getName(), e.getMessage());

        return new ErrorResponse(
            HttpStatus.UNSUPPORTED_MEDIA_TYPE,
            "Unsupported request media type",
            data
        );
    }

    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse noResourceFoundHandler(NoResourceFoundException e){
        log.info("Caught {}: {}", e.getClass().getName(), e.getMessage());

        return new ErrorResponse(
            HttpStatus.NOT_FOUND,
            "Resource not found"
        );
    }

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
