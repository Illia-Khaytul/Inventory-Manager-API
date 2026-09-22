package io.github.khaytul_illia.inventory_manager_api.user;

import io.github.khaytul_illia.inventory_manager_api.TestcontainersConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@DisplayName("UserRepository tests")
public class UserRepositoryTests {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TestEntityManager entityManager;

    @Nested
    @DisplayName("findByUsername tests")
    class FindByUsernameTests{

        @Test
        @DisplayName("Should return empty Optional when user does not exist by username")
        void shouldReturnEmptyOptional_whenUserDoesNotExist(){
            //Act
            Optional<User> foundUser = userRepository.findByUsername("not existent");

            //Assert
            assertThat(foundUser).isEmpty();
        }

        @Test
        @DisplayName("Should return Optional with user when user exists by username")
        void shouldReturnUserOptional_whenUserExists(){
            //Arrange
            User user = new User(null, "username", "password", User.UserRole.CUSTOMER);

            entityManager.persistAndFlush(user);

            //Act
            Optional<User> foundUser = userRepository.findByUsername(user.getUsername());

            //Assert
            assertThat(foundUser).isNotEmpty();
            assertThat(foundUser.get().getUsername()).isEqualTo(user.getUsername());
        }

    }

}
