package io.github.khaytul_illia.inventory_manager_api.user;

import io.github.khaytul_illia.inventory_manager_api.support.context.RepositoryTestContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("UserRepository tests")
public class UserRepositoryTests extends RepositoryTestContext {

    @Autowired
    private UserRepository userRepository;

    @Nested
    @DisplayName("existsByUsername tests")
    class ExistsByUsernameTests {

        @Test
        @DisplayName("Should return false when user does not exist by username")
        void shouldReturnFalse_whenUserDoesNotExist() {
            //Act
            boolean exists = userRepository.existsByUsername("not existent");

            //Assert
            assertThat(exists).isFalse();
        }

        @Test
        @DisplayName("Should return true when user exists by username")
        void shouldReturnTrue_whenUserExists() {
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
    class FindByUsernameTests {

        @Test
        @DisplayName("Should return empty Optional when user does not exist by username")
        void shouldReturnEmptyOptional_whenUserDoesNotExist() {
            //Act
            Optional<User> foundUser = userRepository.findByUsername("not existent");

            //Assert
            assertThat(foundUser).isEmpty();
        }

        @Test
        @DisplayName("Should return Optional with user when user exists by username")
        void shouldReturnUserOptional_whenUserExists() {
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
    class DeleteByUsernameTests {

        @ParameterizedTest
        @MethodSource("provideUsers")
        @DisplayName("Should delete user when it exists by username")
        void shouldDeleteUser_whenExistsByUsername(User target, List<User> otherUsers) {
            //Arrange
            otherUsers.forEach(otherUser -> entityManager.persist(otherUser));
            entityManager.persist(target);
            entityManager.flush();
            entityManager.clear();

            //Act
            userRepository.deleteByUsername(target.getUsername());

            //Assert
            assertThat(userRepository.existsById(target.getId())).isFalse();
            otherUsers.forEach(
                user -> assertThat(userRepository.existsById(user.getId())).isTrue()
            );
        }

        @ParameterizedTest
        @MethodSource("provideUsers")
        @DisplayName("Should do nothing when user does not exist by username")
        void shouldDoNothing_whenUserDoesNotExist(User target, List<User> otherUsers) {
            //Arrange
            otherUsers.forEach(otherUser -> entityManager.persist(otherUser));
            entityManager.flush();
            entityManager.clear();

            //Act
            userRepository.deleteByUsername(target.getUsername());

            //Assert
            otherUsers.forEach(
                user -> assertThat(userRepository.existsById(user.getId())).isTrue()
            );
        }

        /*
                Test data provider methods
         */

        static Stream<Arguments> provideUsers() {
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
