package io.github.khaytul_illia.inventory_manager_api.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class SecurityUtils {

    public Jwt getAuthenticatedUserAccessToken(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if(authentication == null || !authentication.isAuthenticated()){
            throw new IllegalStateException("User is not authenticated");
        }

        Object principal = authentication.getPrincipal();
        if(principal == null){
            throw new IllegalStateException("User principal is null");
        }
        if(!(principal instanceof Jwt)){
            throw new IllegalStateException(String.format("User is authenticated with '%s' instead of a JWT access token", principal.getClass().getSimpleName()));
        }

        return (Jwt) principal;
    }

}
