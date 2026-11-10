package io.github.khaytul_illia.inventory_manager_api.auth.token;

import io.github.khaytul_illia.inventory_manager_api.support.context.RepositoryTestContext;
import io.github.khaytul_illia.inventory_manager_api.auth.session.UserSession;
import io.github.khaytul_illia.inventory_manager_api.support.data.RefreshTokenTestDataFactory;
import io.github.khaytul_illia.inventory_manager_api.support.data.UserSessionTestDataFactory;
import io.github.khaytul_illia.inventory_manager_api.support.data.UserTestDataFactory;
import io.github.khaytul_illia.inventory_manager_api.user.User;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RefreshTokenRepository tests")
public class RefreshTokenRepositoryTests extends RepositoryTestContext {

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Nested
    @DisplayName("findByTokenValue tests")
    class FindByTokenValueTests{

        @ParameterizedTest
        @MethodSource("provideRefreshTokens")
        @DisplayName("Should return refresh token with loaded user session and user when refresh token exists by token value")
        void shouldReturnRefreshTokenAndLoadUserSessionAndUser_whenRefreshTokenExists(RefreshToken target, List<RefreshToken> otherTokens){
            //Arrange
            otherTokens.forEach(refreshToken -> {
                entityManager.persist(refreshToken.getSession().getUser());
                entityManager.persist(refreshToken.getSession());
                entityManager.persist(refreshToken);
            });
            entityManager.persist(target.getSession().getUser());
            entityManager.persist(target.getSession());
            entityManager.persist(target);
            entityManager.flush();
            entityManager.clear();

            //Act
            Optional<RefreshToken> foundToken = refreshTokenRepository.findByTokenValue(target.getTokenValue());

            //Assert
            assertThat(foundToken).isNotEmpty();

            RefreshToken refreshToken = foundToken.get();
            assertThat(Hibernate.isInitialized(refreshToken.getSession())).isTrue();

            UserSession session = refreshToken.getSession();
            assertThat(session.getId()).isEqualTo(target.getSession().getId());
            assertThat(Hibernate.isInitialized(session.getUser())).isTrue();

            User user = session.getUser();
            assertThat(user.getId()).isEqualTo(target.getSession().getUser().getId());
        }

        @ParameterizedTest
        @MethodSource("provideRefreshTokens")
        @DisplayName("Should return nothing when refresh token does not exist by token value")
        void shouldReturnNothing_whenRefreshTokenDoesNotExist(RefreshToken target, List<RefreshToken> otherTokens){
            //Arrange
            otherTokens.forEach(refreshToken -> {
                entityManager.persist(refreshToken.getSession().getUser());
                entityManager.persist(refreshToken.getSession());
                entityManager.persist(refreshToken);
            });
            entityManager.flush();
            entityManager.clear();

            //Act
            Optional<RefreshToken> foundToken = refreshTokenRepository.findByTokenValue(target.getTokenValue());

            //Assert
            assertThat(foundToken).isEmpty();
        }

        /*
                Test data provider methods
         */

        static Stream<Arguments> provideRefreshTokens(){
            return Stream.of(
                //Target refresh token alone
                provideTargetRefreshTokenAlone(),
                //Target and other refresh tokens
                provideTargetAndOtherRefreshTokens()
            );
        }

        private static Arguments provideTargetRefreshTokenAlone(){
            User user = UserTestDataFactory.newDefaultUser();
            UserSession session = UserSessionTestDataFactory.newUserSession(user);
            RefreshToken refreshToken = RefreshTokenTestDataFactory.newDefaultRefreshToken(session);

            return Arguments.of(
                refreshToken, List.of()
            );
        }

        private static Arguments provideTargetAndOtherRefreshTokens(){
            User user = UserTestDataFactory.newDefaultUser();
            UserSession session = UserSessionTestDataFactory.newUserSession(user);

            return Arguments.of(
                RefreshTokenTestDataFactory.newDefaultRefreshToken(session),
                List.of(
                    RefreshTokenTestDataFactory.newUniqueRefreshToken(session),
                    RefreshTokenTestDataFactory.newUniqueRefreshToken(session)
                )
            );
        }

    }

}
