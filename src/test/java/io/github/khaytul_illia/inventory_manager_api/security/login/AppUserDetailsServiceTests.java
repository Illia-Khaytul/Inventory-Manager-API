package io.github.khaytul_illia.inventory_manager_api.security.login;

import io.github.khaytul_illia.inventory_manager_api.user.User;
import io.github.khaytul_illia.inventory_manager_api.user.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AppUserDetailsService tests")
public class AppUserDetailsServiceTests {

    @Mock
    private UserRepository userRepository;
    @InjectMocks
    private AppUserDetailsService userDetailsService;

    @Test
    @DisplayName("Should throw UsernameNotFoundException when user does not exist by username")
    void shouldThrowUsernameNotFoundException_whenUserDoesNotExist(){
        //Arrange
        String username = "username";

        when(userRepository.findByUsername(username))
            .thenReturn(Optional.empty());

        //Act and Arrange
        assertThatThrownBy(() -> userDetailsService.loadUserByUsername(username))
            .isInstanceOf(UsernameNotFoundException.class)
            .hasMessage(String.format("User with username '%s' does not exist", username));

        verify(userRepository).findByUsername(username);
    }

    @Test
    @DisplayName("Should return AppUserDetails when user exists by username")
    void shouldReturnAppUserDetails_whenUserExists(){
        //Arrange
        User user = new User(1L, "username", "password", User.UserRole.CUSTOMER);

        when(userRepository.findByUsername(user.getUsername()))
            .thenReturn(Optional.of(user));

        //Act
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());

        //Assert
        assertThat(userDetails).isNotNull();

        AppUserDetails appUserDetails = (AppUserDetails) userDetails;
        assertThat(appUserDetails.getUser()).isEqualTo(user);

        verify(userRepository).findByUsername(user.getUsername());
    }

}
