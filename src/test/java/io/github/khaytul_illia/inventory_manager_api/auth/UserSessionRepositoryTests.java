package io.github.khaytul_illia.inventory_manager_api.auth;

import io.github.khaytul_illia.inventory_manager_api.TestcontainersConfiguration;
import io.github.khaytul_illia.inventory_manager_api.user.User;
import io.github.khaytul_illia.inventory_manager_api.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@DisplayName("UserSessionRepository tests")
public class UserSessionRepositoryTests {
    
    @Autowired
    private UserSessionRepository sessionRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TestEntityManager entityManager;
    @Autowired
    private TransactionTemplate transactionTemplate;
    
    @Nested
    @DisplayName("countOpenUserSessions test")
    class CountOpenUserSessionsTests{

        @ParameterizedTest
        @MethodSource("provideUserSessions")
        @DisplayName("Should return the number of open user sessions")
        void shouldReturnNumberOfUserSessionsForAUser(List<UserSession> sessions, User owner, int expectedAmount){
            //Arrange
            sessions.forEach(session -> {
                entityManager.persist(session.getUser());
                entityManager.persist(session);
            });
            entityManager.persist(owner);
            entityManager.flush();
            entityManager.clear();

            //Act
            int actualAmount = sessionRepository.countOpenUserSessions(owner.getId());
            
            //Assert
            assertThat(actualAmount).isEqualTo(expectedAmount);
        }

        /*
                Test data provider methods
         */
        
        static Stream<Arguments> provideUserSessions(){
            return Stream.of(
                //Zero sessions at all
                noSessionsCase(),
                //Zero sessions for owner, some for other user
                onlyOtherUserSessionsCase(),
                //Valid sessions for owner and valid for other user
                validOwnerAndOtherUserSessionsCase(),
                //Valid and invalid sessions for owner and valid for other user
                validAndInvalidOwnerSessionsAndValidForOtherUserCase()
            );
        }

        private static Arguments noSessionsCase(){
            User owner = buildUser("username");

            return Arguments.of(
                List.of(), owner, 0
            );
        }

        private static Arguments onlyOtherUserSessionsCase(){
            User owner = buildUser("username");
            User otherUser = buildUser("otherUser");
            Instant now = Instant.now();
            UserSession validOtherUserSession1 = buildSession(true, now, otherUser);
            UserSession validOtherUserSession2 = buildSession(true, now, otherUser);

            return Arguments.of(
                List.of(validOtherUserSession1, validOtherUserSession2), owner, 0
            );
        }

        private static Arguments validOwnerAndOtherUserSessionsCase(){
            User owner = buildUser("username");
            User otherUser = buildUser("otherUser");
            Instant now = Instant.now();
            UserSession validOwnerSession1 = buildSession(true, now, owner);
            UserSession validOwnerSession2 = buildSession(true, now, owner);
            UserSession validOtherUserSession1 = buildSession(true, now, otherUser);
            UserSession validOtherUserSession2 = buildSession(true, now, otherUser);

            return Arguments.of(
                List.of(validOwnerSession1, validOwnerSession2, validOtherUserSession1, validOtherUserSession2), owner, 2
            );
        }

        private static Arguments validAndInvalidOwnerSessionsAndValidForOtherUserCase(){
            User owner = buildUser("username");
            User otherUser = buildUser("otherUser");
            Instant now = Instant.now();
            UserSession validOwnerSession1 = buildSession(true, now, owner);
            UserSession validOwnerSession2 = buildSession(true, now, owner);
            UserSession invalidOwnerSession = buildSession(false, now, owner);
            UserSession expiredOwnerSession = buildSession(true, now.minusSeconds(36000), owner);
            UserSession expiredAndInvalidOwnerSession = buildSession(false, now.minusSeconds(36000), owner);
            UserSession validOtherUserSession1 = buildSession(true, now, otherUser);
            UserSession validOtherUserSession2 = buildSession(true, now, otherUser);

            return Arguments.of(
                List.of(validOwnerSession1, validOwnerSession2, invalidOwnerSession, expiredOwnerSession, expiredAndInvalidOwnerSession,
                    validOtherUserSession1, validOtherUserSession2), owner, 2
            );
        }

    }

    @Nested
    @DisplayName("invalidateSessionById tests")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    class invalidateSessionByIdTests{

        @AfterEach
        void afterEach(){
            transactionTemplate.executeWithoutResult(status -> {
                sessionRepository.deleteAll();
                userRepository.deleteAll();
            });
        }
        
        @ParameterizedTest
        @MethodSource("provideUserSessions")
        @DisplayName("Should invalidate user session when it exists by id")
        void shouldInvalidateSession_whenExistsById(UserSession target, List<UserSession> otherSessions){
            //Arrange
            transactionTemplate.executeWithoutResult(status -> {
                otherSessions.forEach(otherSession -> {
                    entityManager.persist(otherSession.getUser());
                    entityManager.persist(otherSession);
                });
                entityManager.persist(target.getUser());
                entityManager.persist(target);
            });

            //Act
            sessionRepository.invalidateSessionById(target.getId());

            //Assert
            assertThat(sessionRepository.findById(target.getId()).orElseThrow().isValid()).isFalse();
            otherSessions.forEach(
                session -> assertThat(sessionRepository.findById(session.getId()).orElseThrow().isValid()).isTrue()
            );
        }

        @ParameterizedTest
        @MethodSource("provideUserSessions")
        @DisplayName("Should do nothing when user session does not exist by id")
        void shouldDoNothing_whenSessionDoesNotExist(UserSession target, List<UserSession> otherSessions){
            //Arrange
            transactionTemplate.executeWithoutResult(status -> {
                otherSessions.forEach(otherSession -> {
                    entityManager.persist(otherSession.getUser());
                    entityManager.persist(otherSession);
                });
            });

            //Act
            sessionRepository.invalidateSessionById(target.getId());

            //Assert
            otherSessions.forEach(
                session -> assertThat(sessionRepository.findById(session.getId()).orElseThrow().isValid()).isTrue()
            );
        }

        /*
                Test data provider methods
         */

        static Stream<Arguments> provideUserSessions(){
            return Stream.of(
                //Target session alone
                provideTargetSessionAlone(),
                //Target and other sessions
                provideTargetAndOtherSessions()
            );
        }

        private static Arguments provideTargetSessionAlone(){
            Instant now = Instant.now();
            User user = new User(null, "username", "password", User.UserRole.CUSTOMER);
            UserSession session = new UserSession(null, true, now, now.plusSeconds(3600), user);

            return Arguments.of(
                session, List.of()
            );
        }

        private static Arguments provideTargetAndOtherSessions(){
            Instant now = Instant.now();
            User user = new User(null, "username", "password", User.UserRole.CUSTOMER);
            UserSession target = new UserSession(null, true, now, now.plusSeconds(3600), user);
            UserSession other1 = new UserSession(null, true, now, now.plusSeconds(3600), user);
            UserSession other2 = new UserSession(null, true, now, now.plusSeconds(3600), user);

            return Arguments.of(
                target,
                List.of(other1, other2)
            );
        }
        
    }
    
    /*
            Helper methods
     */

    private static User buildUser(String username){
        return new User(null, username, "password", User.UserRole.CUSTOMER);
    }

    private static UserSession buildSession(boolean valid, Instant createdAt, User user){
        return new UserSession(null, valid, createdAt, createdAt.plusSeconds(3600), user);
    }
    
}
