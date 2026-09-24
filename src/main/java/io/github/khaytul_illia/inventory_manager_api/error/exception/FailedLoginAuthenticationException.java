package io.github.khaytul_illia.inventory_manager_api.error.exception;

import org.springframework.security.core.AuthenticationException;

public class FailedLoginAuthenticationException extends RuntimeException {

    public FailedLoginAuthenticationException(AuthenticationException cause) {
        super("Invalid credentials", cause);
    }

}
