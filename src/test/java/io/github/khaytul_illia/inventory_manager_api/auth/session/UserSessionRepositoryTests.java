package io.github.khaytul_illia.inventory_manager_api.auth.session;

import io.github.khaytul_illia.inventory_manager_api.support.context.RepositoryTestContext;
import io.github.khaytul_illia.inventory_manager_api.support.data.TestDataConstants;
import io.github.khaytul_illia.inventory_manager_api.support.data.UserSessionTestDataFactory;
import io.github.khaytul_illia.inventory_manager_api.support.data.UserTestDataFactory;
import io.github.khaytul_illia.inventory_manager_api.user.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("UserSessionRepository tests")
public class UserSessionRepositoryTests extends RepositoryTestContext {

    @Autowired
    private UserSessionRepository sessionRepository;

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
            User owner = UserTestDataFactory.newDefaultUser();

            return Arguments.of(
                List.of(), owner, 0
            );
        }

        private static Arguments onlyOtherUserSessionsCase(){
            User owner = UserTestDataFactory.newDefaultUser();
            User otherUser = UserTestDataFactory.newUniqueUser();

            return Arguments.of(
                List.of(
                    UserSessionTestDataFactory.newUserSession(otherUser),
                    UserSessionTestDataFactory.newUserSession(otherUser)
                ),
                owner, 0
            );
        }

        private static Arguments validOwnerAndOtherUserSessionsCase(){
            User owner = UserTestDataFactory.newDefaultUser();
            User otherUser = UserTestDataFactory.newUniqueUser();

            return Arguments.of(
                List.of(
                    UserSessionTestDataFactory.newUserSession(owner),
                    UserSessionTestDataFactory.newUserSession(owner),
                    UserSessionTestDataFactory.newUserSession(otherUser),
                    UserSessionTestDataFactory.newUserSession(otherUser)
                ),
                owner, 2
            );
        }

        private static Arguments validAndInvalidOwnerSessionsAndValidForOtherUserCase(){
            Instant now = Instant.now();
            User owner = UserTestDataFactory.newDefaultUser();
            User otherUser = UserTestDataFactory.newUniqueUser();

            return Arguments.of(
                List.of(
                    UserSessionTestDataFactory.newUserSession(owner),
                    UserSessionTestDataFactory.newUserSession(owner),
                    UserSessionTestDataFactory.newUserSession(false, now, owner),
                    UserSessionTestDataFactory.newUserSession(true, now.minusSeconds(36000), owner),
                    UserSessionTestDataFactory.newUserSession(false, now.minusSeconds(36000), owner),
                    UserSessionTestDataFactory.newUserSession(otherUser),
                    UserSessionTestDataFactory.newUserSession(otherUser)
                ),
                owner, 2
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
            User user = UserTestDataFactory.newDefaultUser();
            
            return Arguments.of(
                UserSessionTestDataFactory.newUserSession(user), List.of()
            );
        }

        private static Arguments provideTargetAndOtherSessions(){
            User user = UserTestDataFactory.newDefaultUser();

            return Arguments.of(
                UserSessionTestDataFactory.newUserSession(user),
                List.of(
                    UserSessionTestDataFactory.newUserSession(user),
                    UserSessionTestDataFactory.newUserSession(user)
                )
            );
        }
        
    }

    @Nested
    @DisplayName("invalidateAllUserSessions tests")
    class InvalidateAllUserSessionsTests{

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
            sessionRepository.invalidateAllUserSessions(TestDataConstants.USER_USERNAME);

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
            User user = UserTestDataFactory.newDefaultUser();

            return Arguments.of(
                List.of(
                    UserSessionTestDataFactory.newUserSession(user),
                    UserSessionTestDataFactory.newUserSession(user)
                ),
                List.of()
            );
        }

        private static Arguments provideValidAndInvalidOwnedSessions(){
            Instant now = Instant.now();
            User user = UserTestDataFactory.newDefaultUser();

            return Arguments.of(
                List.of(
                    UserSessionTestDataFactory.newUserSession(user),
                    UserSessionTestDataFactory.newUserSession(true, now.minusSeconds(36000), user),
                    UserSessionTestDataFactory.newUserSession(false, now, user)
                ),
                List.of()
            );
        }

        private static Arguments provideValidOwnedAndUnownedSessions(){
            User targetUser = UserTestDataFactory.newDefaultUser();
            User otherUser = UserTestDataFactory.newUniqueUser();

            return Arguments.of(
                List.of(
                    UserSessionTestDataFactory.newUserSession(targetUser),
                    UserSessionTestDataFactory.newUserSession(targetUser)
                ),
                List.of(
                    UserSessionTestDataFactory.newUserSession(otherUser),
                    UserSessionTestDataFactory.newUserSession(otherUser)
                )
            );
        }

    }
    
}
