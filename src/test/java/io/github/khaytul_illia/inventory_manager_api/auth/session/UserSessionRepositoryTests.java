package io.github.khaytul_illia.inventory_manager_api.auth.session;

import io.github.khaytul_illia.inventory_manager_api.TestcontainersConfiguration;
import io.github.khaytul_illia.inventory_manager_api.user.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

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
    private TestEntityManager entityManager;
    
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
    class InvalidateSessionByIdTests{
        
        @ParameterizedTest
        @MethodSource("provideUserSessions")
        @DisplayName("Should invalidate user session when it exists by id")
        void shouldInvalidateSession_whenExistsById(UserSession target, List<UserSession> otherSessions){
            //Arrange
            otherSessions.forEach(otherSession -> {
                entityManager.persist(otherSession.getUser());
                entityManager.persist(otherSession);
            });
            entityManager.persist(target.getUser());
            entityManager.persist(target);
            entityManager.flush();
            entityManager.clear();

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
            otherSessions.forEach(otherSession -> {
                entityManager.persist(otherSession.getUser());
                entityManager.persist(otherSession);
            });
            entityManager.flush();
            entityManager.clear();

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
            User user = buildUser("username");
            UserSession session = buildSession(true, now, user);

            return Arguments.of(
                session, List.of()
            );
        }

        private static Arguments provideTargetAndOtherSessions(){
            Instant now = Instant.now();
            User user = buildUser("username");
            UserSession target = buildSession(true, now, user);
            UserSession other1 = buildSession(true, now, user);
            UserSession other2 = buildSession(true, now, user);

            return Arguments.of(
                target,
                List.of(other1, other2)
            );
        }
        
    }

    @Nested
    @DisplayName("invalidateAllUserSessions tests")
    class InvalidateAllUserSessionsTests{

        private static final String username = "username";

        @ParameterizedTest
        @MethodSource("provideUserSessions")
        @DisplayName("Should invalidate all user sessions")
        void shouldInvalidateAllUserSessions(List<UserSession> targetSessions, List<UserSession> otherSessions){
            //Arrange
            targetSessions.forEach(session -> {
                entityManager.persist(session.getUser());
                entityManager.persist(session);
            });
            otherSessions.forEach(session -> {
                entityManager.persist(session.getUser());
                entityManager.persist(session);
            });
            entityManager.flush();
            entityManager.clear();

            //Act
            sessionRepository.invalidateAllUserSessions(username);

            //Assert
            targetSessions.forEach(
                session -> assertThat(sessionRepository.findById(session.getId()).orElseThrow().isValid()).isFalse()
            );
            otherSessions.forEach(
                session -> assertThat(sessionRepository.findById(session.getId()).orElseThrow().isValid()).isEqualTo(session.isValid())
            );
        }

        /*
                Test data provider methods
         */

        static Stream<Arguments> provideUserSessions(){
            return Stream.of(
                //Only valid owned sessions
                provideValidOwnedSessions(),
                //Valid and invalid owned sessions
                provideValidAndInvalidOwnedSessions(),
                //Valid owned and unowned sessions
                provideValidOwnedAndUnownedSessions()
            );
        }

        private static Arguments provideValidOwnedSessions(){
            Instant now = Instant.now();
            User user = buildUser(username);
            UserSession validOwned1 = buildSession(true, now, user);
            UserSession validOwned2 = buildSession(true, now, user);

            return Arguments.of(
                List.of(validOwned1, validOwned2),
                List.of()
            );
        }

        private static Arguments provideValidAndInvalidOwnedSessions(){
            Instant now = Instant.now();
            User user = buildUser(username);
            UserSession validOwned = buildSession(true, now, user);
            UserSession validExpiredOwned = buildSession(true, now.minusSeconds(36000), user);
            UserSession invalidOwned = buildSession(false, now, user);

            return Arguments.of(
                List.of(validOwned, validExpiredOwned, invalidOwned),
                List.of()
            );
        }

        private static Arguments provideValidOwnedAndUnownedSessions(){
            Instant now = Instant.now();
            User targetUser = buildUser(username);
            User otherUser = buildUser(username + "_other");
            UserSession validOwned1 = buildSession(true, now, targetUser);
            UserSession validOwned2 = buildSession(true, now, targetUser);
            UserSession validUnowned1 = buildSession(true, now, otherUser);
            UserSession validUnowned2 = buildSession(true, now, otherUser);

            return Arguments.of(
                List.of(validOwned1, validOwned2),
                List.of(validUnowned1, validUnowned2)
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
