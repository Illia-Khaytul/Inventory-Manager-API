package io.github.khaytul_illia.inventory_manager_api.security;

import io.github.khaytul_illia.inventory_manager_api.error.exception.EntityNotFoundException;
import io.github.khaytul_illia.inventory_manager_api.user.User;
import io.github.khaytul_illia.inventory_manager_api.user.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Component
public class SecurityUtils {

    private final UserRepository userRepository;

    public SecurityUtils(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public User loadAuthenticatedUser(){
        String username = getAuthenticatedUsername();

        return userRepository.findByUsername(username)
            .orElseThrow(() -> new EntityNotFoundException("Authenticated user '%s' does not exist", username));
    }

    public String getAuthenticatedUsername(){
        Authentication authentication = getAuthenticationToken();

        String authenticatedUsername = authentication.getName();
        if(authenticatedUsername == null || authenticatedUsername.isBlank()){
            throw new IllegalStateException("Authenticated username is not specified");
        }

        return authenticatedUsername;
    }

    public String getAuthenticatedUserRole(){
        Authentication authentication = getAuthenticationToken();

        List<String> foundRoles = authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .filter(Objects::nonNull)
            .filter(authority -> authority.startsWith("ROLE_"))
            .toList();

        int roleCount = foundRoles.size();
        if(roleCount == 0){
            throw new IllegalStateException("Authenticated user has no role");
        }
        if(roleCount > 1){
            throw new IllegalStateException("Authenticated user has more than 1 role " + foundRoles);
        }

        return foundRoles.getFirst().substring(5);
    }

    public Authentication getAuthenticationToken(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if(authentication == null || !authentication.isAuthenticated()){
            throw new IllegalStateException("User is not authenticated");
        }

        return authentication;
    }

}
