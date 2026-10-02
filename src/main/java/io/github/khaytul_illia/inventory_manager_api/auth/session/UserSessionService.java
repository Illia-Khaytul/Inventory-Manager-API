package io.github.khaytul_illia.inventory_manager_api.auth.session;

import io.github.khaytul_illia.inventory_manager_api.error.exception.UserSessionLimitExceededException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@Slf4j
public class UserSessionService {

    private final int maxOpenUserSessions;

    private final UserSessionRepository sessionRepository;
    private final UserSessionFactory sessionFactory;

    public UserSessionService(
        @Value("${spring.application.security.user_sessions.limit}")
        int maxOpenUserSessions,
        UserSessionRepository sessionRepository,
        UserSessionFactory sessionFactory
    ) {
        this.maxOpenUserSessions = maxOpenUserSessions;

        this.sessionRepository = sessionRepository;
        this.sessionFactory = sessionFactory;
    }

    @Transactional
    public UserSession createUserSession(Long userId){
        log.debug("Checking if user has not reached maximum open session limit");

        if(sessionRepository.countOpenUserSessions(userId) >= maxOpenUserSessions){
            throw new UserSessionLimitExceededException("Maximum amount of user sessions opened (%s)", maxOpenUserSessions);
        }

        log.debug("Opening new user session");

        Instant now = Instant.now();
        UserSession session = sessionFactory.buildUserSession(now, userId);

        return sessionRepository.save(session);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void invalidateSession(Long sessionId){
        log.debug("Invalidating user session");

        sessionRepository.invalidateSessionById(sessionId);
    }

    @Transactional
    public void invalidateAllUserSessions(String username){
        log.debug("Invalidating all user sessions");

        sessionRepository.invalidateAllUserSessions(username);
    }

}
