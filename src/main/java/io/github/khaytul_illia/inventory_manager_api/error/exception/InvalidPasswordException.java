package io.github.khaytul_illia.inventory_manager_api.error.exception;

import lombok.Getter;

import java.util.List;

@Getter
public class InvalidPasswordException extends RuntimeException {

    private final List<String> errorMessages;

    public InvalidPasswordException(String message, List<String> errorMessages) {
        super(message);

        this.errorMessages = errorMessages;
    }

}
