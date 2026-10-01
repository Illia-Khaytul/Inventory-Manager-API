package io.github.khaytul_illia.inventory_manager_api.auth.session;

import io.github.khaytul_illia.inventory_manager_api.user.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class UserSessionFactory {

    private final int userSessionLifetime;

    private final UserRepository userRepository;

    public UserSessionFactory(
        @Value("${spring.application.security.user_sessions.lifetime}")
        int userSessionLifetime,
        UserRepository userRepository
    ) {
        this.userSessionLifetime = userSessionLifetime;

        this.userRepository = userRepository;
    }

    public UserSession buildUserSession(Instant createdAt, Long userId){
        UserSession session = new UserSession();
        session.setValid(true);
        session.setCreatedAt(createdAt);
        session.setExpiresAt(createdAt.plusSeconds(userSessionLifetime));
        session.setUser(userRepository.getReferenceById(userId));

        return session;
    }

}
