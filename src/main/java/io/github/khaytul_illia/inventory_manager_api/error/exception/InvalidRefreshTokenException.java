package io.github.khaytul_illia.inventory_manager_api.error.exception;

import lombok.Getter;

import java.util.List;

@Getter
public class InvalidRefreshTokenException extends RuntimeException {

    private final List<String> details;

    public InvalidRefreshTokenException(String message, List<String> details) {
        super(message);

        this.details = details;
    }

}
