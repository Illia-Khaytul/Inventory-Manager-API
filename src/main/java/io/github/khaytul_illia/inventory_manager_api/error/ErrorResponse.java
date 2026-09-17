package io.github.khaytul_illia.inventory_manager_api.error;

import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.Map;

public record ErrorResponse(
    Instant timestamp,
    int status,
    String message,
    Map<String, Object> data
) {

    public ErrorResponse(HttpStatus status, String message){
        this(Instant.now(), status.value(), message, Map.of());
    }

    public ErrorResponse(HttpStatus status, String message, Map<String, Object> data){
        this(Instant.now(), status.value(), message, data);
    }

}
