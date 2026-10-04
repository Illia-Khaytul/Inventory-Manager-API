package io.github.khaytul_illia.inventory_manager_api.user;

import io.github.khaytul_illia.inventory_manager_api.user.request.CreateUserRequest;
import io.github.khaytul_illia.inventory_manager_api.user.response.UserResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@ConditionalOnBooleanProperty(
    name = "apring.application.base_operator.seeder.enable",
    matchIfMissing = true
)
@Slf4j
public class BaseOperatorSeeder implements CommandLineRunner {

    private final String baseUsername;
    private final String basePassword;

    private final UserService userService;
    private final UserRepository userRepository;

    public BaseOperatorSeeder(
        @Value("${spring.application.base_operator.username}")
        String baseUsername,
        @Value("${spring.application.base_operator.password}")
        String basePassword,
        UserService userService,
        UserRepository userRepository
    ) {
        if(baseUsername == null || baseUsername.isBlank() || basePassword == null || basePassword.isBlank()){
            throw new IllegalStateException("Base OPERATOR user credentials cannot be null nor blank");
        }

        this.baseUsername = baseUsername;
        this.basePassword = basePassword;
        this.userService = userService;
        this.userRepository = userRepository;
    }

    @Override
    public void run(String... args) {
        log.info("Creating base OPERATOR user");

        Optional<User> foundBaseUser = userRepository.findByUsername(baseUsername);
        if(foundBaseUser.isPresent()){
            User baseUser = foundBaseUser.get();
            if(!baseUser.getRole().equals(User.UserRole.OPERATOR)){
                throw new IllegalStateException(String.format("User '%s' with id %s exists but is not an OPERATOR", baseUser.getUsername(), baseUser.getId()));
            }

            log.info("Base OPERATOR user already exists");

            return;
        }

        CreateUserRequest request = new CreateUserRequest(baseUsername, basePassword);
        UserResponse baseUserData = userService.createUser(request, User.UserRole.OPERATOR);

        log.info("Base OPERATOR user '{}' successfully created with id {}", baseUsername, baseUserData.id());
    }

}
