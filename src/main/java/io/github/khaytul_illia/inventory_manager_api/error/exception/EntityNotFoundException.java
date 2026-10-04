package io.github.khaytul_illia.inventory_manager_api.error.exception;

public class EntityNotFoundException extends RuntimeException {

    public EntityNotFoundException(String message) {
        super(message);
    }

    public EntityNotFoundException(String message, Object... args) {
        super(String.format(message, args));
    }

}
