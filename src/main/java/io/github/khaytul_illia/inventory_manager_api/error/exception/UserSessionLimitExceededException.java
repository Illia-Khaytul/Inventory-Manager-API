package io.github.khaytul_illia.inventory_manager_api.error.exception;

public class UserSessionLimitExceededException extends RuntimeException {

    public UserSessionLimitExceededException(String message) {
        super(message);
    }

    public UserSessionLimitExceededException(String message, Object... args) {
        super(String.format(message, args));
    }

}
