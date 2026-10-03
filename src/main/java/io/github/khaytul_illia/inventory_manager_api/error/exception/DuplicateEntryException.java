package io.github.khaytul_illia.inventory_manager_api.error.exception;

public class DuplicateEntryException extends RuntimeException {

    public DuplicateEntryException(String message) {
        super(message);
    }

    public DuplicateEntryException(String message, Object... args) {
        super(String.format(message, args));
    }

}
