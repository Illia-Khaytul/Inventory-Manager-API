package io.github.khaytul_illia.inventory_manager_api.error.exception;

public class UserSessionLimitExceededException extends RuntimeException {

    public UserSessionLimitExceededException(String message) {
        super(message);
    }

}
