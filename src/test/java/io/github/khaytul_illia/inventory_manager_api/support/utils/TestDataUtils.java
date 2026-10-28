package io.github.khaytul_illia.inventory_manager_api.support.utils;

import io.github.khaytul_illia.inventory_manager_api.auth.session.UserSession;
import io.github.khaytul_illia.inventory_manager_api.user.User;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;

public class TestDataUtils {

    public static void persistAuditedEntities(User auditor, Runnable operation){
        SecurityContextHolder.getContext().setAuthentication(
            new TestingAuthenticationToken(auditor.getUsername(), auditor.getPassword(), "ROLE_" + auditor.getRole().name())
        );

        operation.run();

        SecurityContextHolder.getContext().setAuthentication(null);
    }

    public static User buildUser(String username){
        return new User(null, username, "password", User.UserRole.CUSTOMER);
    }

    public static UserSession buildSession(boolean valid, Instant createdAt, User user){
        return new UserSession(null, valid, createdAt, createdAt.plusSeconds(3600), user);
    }

}
