package io.github.khaytul_illia.inventory_manager_api.auth.session;

import io.github.khaytul_illia.inventory_manager_api.error.exception.UserSessionLimitExceededException;
import io.github.khaytul_illia.inventory_manager_api.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserSessionService tests")
public class UserSessionServiceTests {

    private final int maxOpenUserSessions = 10;
    @Mock
    private UserSessionRepository sessionRepository;
    @Mock
    private UserSessionFactory sessionFactory;
    private UserSessionService sessionService;

    @BeforeEach
    void beforeEach(){
        sessionService = new UserSessionService(
            maxOpenUserSessions,
            sessionRepository,
            sessionFactory
        );
    }

    @Nested
    @DisplayName("createUserSession tests")
    class CreateUserSessionTests{

        private final Long userId = 1L;

        @ParameterizedTest
        @ValueSource(ints = {10, 11})
        @DisplayName("Should throw UserSessionLimitExceededException when user has already opened a max amount of sessions")
        void shouldThrowUserSessionLimitExceededException_whenSessionLimitReached(int openedSessions){
            //Arrange
            when(sessionRepository.countOpenUserSessions(userId))
                .thenReturn(openedSessions);

            //Act and Assert
            assertThatThrownBy(() -> sessionService.createUserSession(userId))
                .isInstanceOf(UserSessionLimitExceededException.class)
                .hasMessage(String.format("Maximum amount of user sessions opened (%s)", maxOpenUserSessions));

            verify(sessionRepository).countOpenUserSessions(userId);
        }

        @Test
        @DisplayName("Should create and persist a new user session when the user has not reached the max open session limit")
        void shouldCreateAndPersistNewSession_whenOpenSessionLimitIsNotReached(){
            //Arrange
            User user = new User(1L, "username", "password", User.UserRole.CUSTOMER);
            Instant now = Instant.now();
            UserSession session = new UserSession(1L, true, now, now.plusSeconds(3600), user);

            when(sessionRepository.countOpenUserSessions(userId))
                .thenReturn(0);
            when(sessionFactory.buildUserSession(any(Instant.class), anyLong()))
                .thenReturn(session);
            when(sessionRepository.save(session))
                .thenReturn(session);

            //Act
            UserSession newSession = sessionService.createUserSession(userId);

            //Assert
            assertThat(newSession).isNotNull();
            assertThat(newSession.getId()).isEqualTo(session.getId());

            verify(sessionRepository).countOpenUserSessions(userId);
            verify(sessionFactory).buildUserSession(any(Instant.class), anyLong());
            verify(sessionRepository).save(session);
        }

    }

    @Nested
    @DisplayName("invalidateSession tests")
    class InvalidateSessionTests{

        private final Long sessionId = 1L;

        @Test
        @DisplayName("Should invalidate user session by id")
        void shouldInvalidateSessionById(){
            //Arrange
            doNothing()
                .when(sessionRepository).invalidateSessionById(sessionId);

            //Act
            sessionService.invalidateSession(sessionId);

            //Assert
            verify(sessionRepository).invalidateSessionById(sessionId);
        }

    }

    @Nested
    @DisplayName("invalidateAllUserSessions tests")
    class InvalidateAllUserSessionsTests{

        private final String username = "username";

        @Test
        @DisplayName("Should invalidate all user sessions by username")
        void shouldInvalidateAllUserSssionsByUsername(){
            //Arrange
            doNothing()
                .when(sessionRepository).invalidateAllUserSessions(username);

            //Act
            sessionService.invalidateAllUserSessions(username);

            //Assert
            verify(sessionRepository).invalidateAllUserSessions(username);
        }

    }

}
