package io.github.khaytul_illia.inventory_manager_api.error;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.Map;

@Schema(
    description = "Response containing information about the occurred error",
    example = """
        {
            "timestamp": "2026-09-24T10:33:00Z",
            "status": 500,
            "message": "Something went wrong",
            "data": {}
        }
        """
)
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
