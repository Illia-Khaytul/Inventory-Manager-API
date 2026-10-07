package io.github.khaytul_illia.inventory_manager_api.user;

import io.github.khaytul_illia.inventory_manager_api.error.exception.DuplicateEntryException;
import io.github.khaytul_illia.inventory_manager_api.error.exception.InvalidPasswordException;
import io.github.khaytul_illia.inventory_manager_api.security.SecurityUtils;
import io.github.khaytul_illia.inventory_manager_api.user.password.UserPasswordValidator;
import io.github.khaytul_illia.inventory_manager_api.user.request.CreateUserRequest;
import io.github.khaytul_illia.inventory_manager_api.user.request.PasswordChangeRequest;
import io.github.khaytul_illia.inventory_manager_api.user.response.UserResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final UserPasswordValidator passwordValidator;
    private final SecurityUtils securityUtils;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    public UserService(
        UserRepository userRepository,
        UserPasswordValidator passwordValidator,
        SecurityUtils securityUtils,
        PasswordEncoder passwordEncoder,
        UserMapper userMapper
    ) {
        this.userRepository = userRepository;
        this.passwordValidator = passwordValidator;
        this.securityUtils = securityUtils;
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

        UserResponse userResponse = userMapper.toUserResponse(user);

        log.info("Successfully created new user ({}) with id {}", role.name(), user.getId());

        return userResponse;
    }

    @Transactional
    public void changePassword(PasswordChangeRequest request){
        String newPassword = request.newPassword();
        String oldPassword = request.oldPassword();

        log.info("Password change attempt");

        log.debug("Validating new password");

        if(oldPassword.equals(newPassword)){
            throw new InvalidPasswordException("Invalid password", List.of("New password cannot be the same as old password."));
        }

        passwordValidator.validateUserPassword(newPassword);

        log.debug("Checking if provided old password and existing old password match");

        User user = securityUtils.loadAuthenticatedUser();
        if(!passwordEncoder.matches(oldPassword, user.getPassword())){
            throw new InvalidPasswordException("Invalid password change attempt", List.of("Provided old password must match existing old password."));
        }

        log.debug("Changing user password");

        user.setPassword(passwordEncoder.encode(newPassword));

        log.info("Password changed successfully for user with id {}", user.getId());
    }

    @Transactional
    public void deleteUser(){
        log.info("Deleting user account");

        String username = securityUtils.getAuthenticatedUserAccessToken().getSubject();

        userRepository.deleteByUsername(username);

        log.info("Successfully deleted user '{}'", username);
    }

}
