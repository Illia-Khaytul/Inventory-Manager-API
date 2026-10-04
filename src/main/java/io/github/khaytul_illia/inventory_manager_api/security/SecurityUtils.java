package io.github.khaytul_illia.inventory_manager_api.security;

import io.github.khaytul_illia.inventory_manager_api.error.exception.EntityNotFoundException;
import io.github.khaytul_illia.inventory_manager_api.user.User;
import io.github.khaytul_illia.inventory_manager_api.user.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class SecurityUtils {

    private final UserRepository userRepository;

    public SecurityUtils(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public User loadAuthenticatedUser(){
        String username = getAuthenticatedUserAccessToken().getSubject();

        return userRepository.findByUsername(username)
            .orElseThrow(() -> new EntityNotFoundException("Authenticated user '%s' does not exist", username));
    }

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
