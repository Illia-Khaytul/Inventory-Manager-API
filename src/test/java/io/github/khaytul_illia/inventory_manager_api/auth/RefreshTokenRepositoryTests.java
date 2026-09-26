package io.github.khaytul_illia.inventory_manager_api.auth;

import io.github.khaytul_illia.inventory_manager_api.TestcontainersConfiguration;
import io.github.khaytul_illia.inventory_manager_api.user.User;
import org.hibernate.Hibernate;
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
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@DisplayName("RefreshTokenRepository tests")
public class RefreshTokenRepositoryTests {

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;
    @Autowired
    private TestEntityManager entityManager;

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
            Instant now = Instant.now();
            User user = new User(null, "username", "password", User.UserRole.CUSTOMER);
            UserSession session = new UserSession(null, true, now, now.plusSeconds(3600), user);
            RefreshToken refreshToken = new RefreshToken(null, "refresh token value", now, false, session, 1);

            return Arguments.of(
                refreshToken, List.of()
            );
        }

        private static Arguments provideTargetAndOtherRefreshTokens(){
            Instant now = Instant.now();
            User user = new User(null, "username", "password", User.UserRole.CUSTOMER);
            UserSession session = new UserSession(null, true, now, now.plusSeconds(3600), user);
            RefreshToken target = new RefreshToken(null, "refresh token value 1", now, false, session, 1);
            RefreshToken other1 = new RefreshToken(null, "refresh token value 2", now, false, session, 1);
            RefreshToken other2 = new RefreshToken(null, "refresh token value 3", now, false, session, 1);

            return Arguments.of(
                target,
                List.of(other1, other2)
            );
        }

    }

}
