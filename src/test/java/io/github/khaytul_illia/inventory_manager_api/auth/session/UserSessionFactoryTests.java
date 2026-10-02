package io.github.khaytul_illia.inventory_manager_api.auth.session;

import io.github.khaytul_illia.inventory_manager_api.user.User;
import io.github.khaytul_illia.inventory_manager_api.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserSessionFactory tests")
public class UserSessionFactoryTests {

    private final int userSessionLifetime = 3600;
    @Mock
    private UserRepository userRepository;
    private UserSessionFactory sessionFactory;

    @BeforeEach
    void beforeEach(){
        sessionFactory = new UserSessionFactory(
            userSessionLifetime,
            userRepository
        );
    }

    @ParameterizedTest
    @CsvSource(
        textBlock = """
            2026-10-01T18:20:00Z, 1
            2026-10-11T12:56:00Z, 5
            """
    )
    @DisplayName("Should build new UserSession")
    void shouldBuildUserSession(String createdAtString, Long userId){
        //Arrange
        Instant createdAt = Instant.parse(createdAtString);
        User referenceMock = mock(User.class);

        when(userRepository.getReferenceById(userId))
            .thenReturn(referenceMock);
        when(referenceMock.getId())
            .thenReturn(userId);

        //Act
        UserSession session = sessionFactory.buildUserSession(createdAt, userId);

        //Assert
        assertThat(session).isNotNull();
        assertThat(session.getId()).isNull();
        assertThat(session.isValid()).isTrue();
        assertThat(session.getCreatedAt()).isEqualTo(createdAt);
        assertThat(session.getExpiresAt()).isEqualTo(createdAt.plusSeconds(userSessionLifetime));
        assertThat(session.getUser().getId()).isEqualTo(userId);

        verify(userRepository).getReferenceById(userId);
    }

}
