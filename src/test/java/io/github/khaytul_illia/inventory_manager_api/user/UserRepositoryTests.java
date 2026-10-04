package io.github.khaytul_illia.inventory_manager_api.user;

import io.github.khaytul_illia.inventory_manager_api.TestcontainersConfiguration;
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

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

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
    @DisplayName("existsByUsername tests")
    class ExistsByUsernameTests{

        @Test
        @DisplayName("Should return false when user does not exist by username")
        void shouldReturnFalse_whenUserDoesNotExist(){
            //Act
            boolean exists = userRepository.existsByUsername("not existent");

            //Assert
            assertThat(exists).isFalse();
        }

        @Test
        @DisplayName("Should return true when user exists by username")
        void shouldReturnTrue_whenUserExists(){
            //Arrange
            User user = new User(null, "username", "password", User.UserRole.CUSTOMER);

            entityManager.persistAndFlush(user);
            entityManager.clear();

            //Act
            boolean exists = userRepository.existsByUsername(user.getUsername());

            //Assert
            assertThat(exists).isTrue();
        }

    }

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
            entityManager.clear();

            //Act
            Optional<User> foundUser = userRepository.findByUsername(user.getUsername());

            //Assert
            assertThat(foundUser).isNotEmpty();
            assertThat(foundUser.get().getUsername()).isEqualTo(user.getUsername());
        }

    }

    @Nested
    @DisplayName("deleteByUsername tests")
    class DeleteByUsernameTests{

        @ParameterizedTest
        @MethodSource("provideUsers")
        @DisplayName("Should delete user when it exists by username")
        void shouldDeleteUser_whenExistsByUsername(User target, List<User> otherUsers){
            //Arrange
            otherUsers.forEach(otherUser -> {
                entityManager.persist(otherUser);
            });
            entityManager.persist(target);
            entityManager.flush();
            entityManager.clear();

            //Act
            userRepository.deleteByUsername(target.getUsername());

            //Assert
            assertThat(userRepository.findById(target.getId())).isEmpty();
            otherUsers.forEach(
                user -> assertThat(userRepository.findById(user.getId())).isNotEmpty()
            );
        }

        @ParameterizedTest
        @MethodSource("provideUsers")
        @DisplayName("Should do nothing when user does not exist by username")
        void shouldDoNothing_whenUserDoesNotExist(User target, List<User> otherUsers){
            //Arrange
            otherUsers.forEach(otherUser -> {
                entityManager.persist(otherUser);
            });
            entityManager.flush();
            entityManager.clear();

            //Act
            userRepository.deleteByUsername(target.getUsername());

            //Assert
            otherUsers.forEach(
                user -> assertThat(userRepository.findById(user.getId())).isNotEmpty()
            );
        }

        /*
                Test data provider methods
         */

        static Stream<Arguments> provideUsers(){
            return Stream.of(
                //Target session alone
                Arguments.of(
                    new User(null, "username", "password", User.UserRole.CUSTOMER),
                    List.of()
                ),
                //Target and other sessions
                Arguments.of(
                    new User(null, "username", "password", User.UserRole.CUSTOMER),
                    List.of(
                        new User(null, "other1", "password", User.UserRole.CUSTOMER),
                        new User(null, "other2", "password", User.UserRole.CUSTOMER)
                    )
                )
            );
        }

    }

}
