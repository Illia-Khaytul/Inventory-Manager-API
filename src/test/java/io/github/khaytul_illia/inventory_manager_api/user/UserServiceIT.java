package io.github.khaytul_illia.inventory_manager_api.user;

import io.github.khaytul_illia.inventory_manager_api.TestcontainersConfiguration;
import io.github.khaytul_illia.inventory_manager_api.user.request.CreateUserRequest;
import io.github.khaytul_illia.inventory_manager_api.user.response.UserResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@DisplayName("UserService integration tests")
public class UserServiceIT {

    @Autowired
    private UserService userService;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private UserRepository userRepository;

    private final String password = "password55";
    private User user;

    @BeforeEach
    void beforeEach(){
        user = userRepository.save(new User(null, "username", passwordEncoder.encode(password), User.UserRole.CUSTOMER));
    }

    @AfterEach
    void afterEach(){
        userRepository.deleteAll();
    }

    @Nested
    @DisplayName("createUser integration tests")
    class CreateUserIT {

        @ParameterizedTest
        @EnumSource(User.UserRole.class)
        @DisplayName("Should create new user with provided role when provided username is not taken")
        void shouldCreateNewUser_whenUsernameIsUnique(User.UserRole role){
            //Arrange
            CreateUserRequest request = new CreateUserRequest(user.getUsername() + "_different", password);

            //Act
            UserResponse response = userService.createUser(request, role);

            //Assert
            assertThat(response).isNotNull();
            assertThat(response.role()).isEqualTo(role);
            assertThat(userRepository.count()).isEqualTo(2);
            assertThat(userRepository.findByUsername(request.username()).orElseThrow().getRole()).isEqualTo(role);
        }

    }

}
