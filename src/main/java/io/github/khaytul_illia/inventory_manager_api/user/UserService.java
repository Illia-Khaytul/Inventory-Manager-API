package io.github.khaytul_illia.inventory_manager_api.user;

import io.github.khaytul_illia.inventory_manager_api.error.exception.DuplicateEntryException;
import io.github.khaytul_illia.inventory_manager_api.user.password.UserPasswordValidator;
import io.github.khaytul_illia.inventory_manager_api.user.request.CreateUserRequest;
import io.github.khaytul_illia.inventory_manager_api.user.response.UserResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final UserPasswordValidator passwordValidator;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    public UserService(
        UserRepository userRepository,
        UserPasswordValidator passwordValidator,
        PasswordEncoder passwordEncoder,
        UserMapper userMapper
    ) {
        this.userRepository = userRepository;
        this.passwordValidator = passwordValidator;
        this.passwordEncoder = passwordEncoder;
        this.userMapper = userMapper;
    }

    public UserResponse createUser(CreateUserRequest request, User.UserRole role){
        String username = request.username();
        String password = request.password();

        log.info("Creating new user ({}) with username '{}'", role.name(), username);

        log.debug("Validating user password");

        passwordValidator.validateUserPassword(password);

        log.debug("Checking username uniqueness");

        if(userRepository.existsByUsername(username)){
            throw new DuplicateEntryException("Username '%s' is not unique", username);
        }

        log.debug("Creating new user");

        User user = userMapper.buildUser(username, passwordEncoder.encode(password), role);

        user = userRepository.save(user);

        log.info("Successfully created new user ({}) with id {}", role.name(), user.getId());

        return userMapper.toUserResponse(user);
    }

}
