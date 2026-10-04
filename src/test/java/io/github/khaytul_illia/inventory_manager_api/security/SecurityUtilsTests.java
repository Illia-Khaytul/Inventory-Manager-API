package io.github.khaytul_illia.inventory_manager_api.security;

import io.github.khaytul_illia.inventory_manager_api.error.exception.EntityNotFoundException;
import io.github.khaytul_illia.inventory_manager_api.security.login.AppUserDetails;
import io.github.khaytul_illia.inventory_manager_api.user.User;
import io.github.khaytul_illia.inventory_manager_api.user.UserRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SecurityUtils tests")
public class SecurityUtilsTests {

    @Mock
    private UserRepository userRepository;
    @InjectMocks
    private SecurityUtils securityUtils;

    @Nested
    @DisplayName("loadAuthenticatedUser tests")
    class LoadAuthenticatedUserTests{

        private final String username = "username";

        private SecurityUtils securityUtilsSpy;

        @BeforeEach
        void beforeEach(){
            securityUtilsSpy = spy(securityUtils);
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when the authenticated user does not exist")
        void shouldThrowEntityNotFoundException_whenAuthenticatedUserDoesNotExist(){
            //Arrange
            Jwt jwtMock = mock(Jwt.class);

            doReturn(jwtMock)
                .when(securityUtilsSpy).getAuthenticatedUserAccessToken();
            when(jwtMock.getSubject())
                .thenReturn(username);
            when(userRepository.findByUsername(username))
                .thenReturn(Optional.empty());

            //Act and Assert
            assertThatThrownBy(() -> securityUtilsSpy.loadAuthenticatedUser())
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage(String.format("Authenticated user '%s' does not exist", username));

            verify(securityUtilsSpy).getAuthenticatedUserAccessToken();
        }

        @Test
        @DisplayName("Should return the authenticated user when it exists")
        void shouldReturnAuthenticatedUser_whenItExists(){
            //Arrange
            Jwt jwtMock = mock(Jwt.class);
            User foundUser = new User(1L, username, "password", User.UserRole.CUSTOMER);

            doReturn(jwtMock)
                .when(securityUtilsSpy).getAuthenticatedUserAccessToken();
            when(jwtMock.getSubject())
                .thenReturn(username);
            when(userRepository.findByUsername(username))
                .thenReturn(Optional.of(foundUser));

            //Act
            User user = securityUtilsSpy.loadAuthenticatedUser();

            //Assert
            assertThat(user).isNotNull();
            assertThat(user.getUsername()).isEqualTo(username);

            verify(securityUtilsSpy).getAuthenticatedUserAccessToken();
            verify(userRepository).findByUsername(username);
        }

    }

    @Nested
    @DisplayName("getAuthenticatedUserAccessToken tests")
    class GetAuthenticatedUserAccessTokenTests{

        private MockedStatic<SecurityContextHolder> securityContextHolder;
        private SecurityContext securityContext;

        @BeforeEach
        void beforeEach(){
            securityContextHolder = mockStatic(SecurityContextHolder.class);
            securityContext = mock(SecurityContext.class);
        }

        @AfterEach
        void afterEach(){
            securityContextHolder.close();
        }

        @ParameterizedTest
        @NullSource
        @MethodSource("provideInvalidAuthentications")
        @DisplayName("Should throw IllegalStateException when authentication is null or not authenticated")
        void shouldThrowIllegalStateException_whenAuthenticationNullOrNotAuthenticated(Authentication authentication){
            //Arrange
            securityContextHolder.when(SecurityContextHolder::getContext)
                .thenReturn(securityContext);
            when(securityContext.getAuthentication())
                .thenReturn(authentication);

            //Act and Assert
            assertThatThrownBy(() -> securityUtils.getAuthenticatedUserAccessToken())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("User is not authenticated");

            securityContextHolder.verify(SecurityContextHolder::getContext);
            verify(securityContext).getAuthentication();
        }

        @ParameterizedTest
        @MethodSource("provideInvalidPrincipals")
        @DisplayName("Should throw IllegalStateException when authentication principal is null or not a JWT")
        void shouldThrowIllegalStateException_whenPrincipalNullOrNotJwt(Object principal, String errorMessage){
            //Arrange
            Authentication authentication = new TestingAuthenticationToken(principal, "password", User.UserRole.CUSTOMER.name());

            securityContextHolder.when(SecurityContextHolder::getContext)
                .thenReturn(securityContext);
            when(securityContext.getAuthentication())
                .thenReturn(authentication);

            //Act and Assert
            assertThatThrownBy(() -> securityUtils.getAuthenticatedUserAccessToken())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(errorMessage);

            securityContextHolder.verify(SecurityContextHolder::getContext);
            verify(securityContext).getAuthentication();
        }

        @Test
        @DisplayName("Should return the JWT access token when user authenticated with JWT")
        void shouldReturnJwt_whenUserAuthenticatedWithJwt(){
            //Arrange
            Jwt jwt = mock(Jwt.class);
            Authentication authentication = new TestingAuthenticationToken(jwt, "password", User.UserRole.CUSTOMER.name());

            securityContextHolder.when(SecurityContextHolder::getContext)
                .thenReturn(securityContext);
            when(securityContext.getAuthentication())
                .thenReturn(authentication);
            when(jwt.getSubject())
                .thenReturn("username");

            //Act
            Jwt result = securityUtils.getAuthenticatedUserAccessToken();

            //Assert
            assertThat(result).isNotNull();
            assertThat(result.getSubject()).isEqualTo(jwt.getSubject());

            securityContextHolder.verify(SecurityContextHolder::getContext);
            verify(securityContext).getAuthentication();
        }

        /*
                Test data provider methods
         */

        static Stream<Arguments> provideInvalidAuthentications(){
            return Stream.of(
                //Not authenticated authentication
                Arguments.of(new UsernamePasswordAuthenticationToken("username", "password"))
            );
        }

        static Stream<Arguments> provideInvalidPrincipals(){
            return Stream.of(
                //Null principal
                Arguments.of(
                    null,
                    "User principal is null"
                ),
                //UserDetails principal
                Arguments.of(
                    new AppUserDetails(new User(1L, "username", "password", User.UserRole.CUSTOMER)),
                    String.format("User is authenticated with '%s' instead of a JWT access token", AppUserDetails.class.getSimpleName())
                ),
                //String principal
                Arguments.of(
                    "username",
                    String.format("User is authenticated with '%s' instead of a JWT access token", String.class.getSimpleName())
                )
            );
        }

    }

}
