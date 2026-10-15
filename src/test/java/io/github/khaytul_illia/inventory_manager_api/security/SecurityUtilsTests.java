package io.github.khaytul_illia.inventory_manager_api.security;

import io.github.khaytul_illia.inventory_manager_api.error.exception.EntityNotFoundException;
import io.github.khaytul_illia.inventory_manager_api.user.User;
import io.github.khaytul_illia.inventory_manager_api.user.UserRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collection;
import java.util.List;
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
    private SecurityUtils securityUtilsSpy;

    @BeforeEach
    void beforeEach(){
        securityUtilsSpy = spy(securityUtils);
    }

    @Nested
    @DisplayName("loadAuthenticatedUser tests")
    class LoadAuthenticatedUserTests{

        private final String username = "username";

        @Test
        @DisplayName("Should throw EntityNotFoundException when the authenticated user does not exist")
        void shouldThrowEntityNotFoundException_whenAuthenticatedUserDoesNotExist(){
            //Arrange
            doReturn(username)
                .when(securityUtilsSpy).getAuthenticatedUsername();
            when(userRepository.findByUsername(username))
                .thenReturn(Optional.empty());

            //Act and Assert
            assertThatThrownBy(() -> securityUtilsSpy.loadAuthenticatedUser())
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage(String.format("Authenticated user '%s' does not exist", username));

            verify(securityUtilsSpy).getAuthenticatedUsername();
            verify(userRepository).findByUsername(username);
        }

        @Test
        @DisplayName("Should return the authenticated user when it exists")
        void shouldReturnAuthenticatedUser_whenItExists(){
            //Arrange
            User foundUser = new User(1L, username, "password", User.UserRole.CUSTOMER);

            doReturn(username)
                .when(securityUtilsSpy).getAuthenticatedUsername();
            when(userRepository.findByUsername(username))
                .thenReturn(Optional.of(foundUser));

            //Act
            User user = securityUtilsSpy.loadAuthenticatedUser();

            //Assert
            assertThat(user).isNotNull();
            assertThat(user.getUsername()).isEqualTo(username);

            verify(securityUtilsSpy).getAuthenticatedUsername();
            verify(userRepository).findByUsername(username);
        }

    }

    @Nested
    @DisplayName("getAuthenticatedUsername tests")
    class GetAuthenticatedUsernameTests{

        @ParameterizedTest
        @NullSource
        @ValueSource(strings = {"", "   "})
        @DisplayName("Should throw IllegalStateException when authenticated username is null or blank")
        void shouldThrowIllegalStateException_whenAuthenticatedUsernameNotSpecified(String username){
            //Arrange
            TestingAuthenticationToken authentication = mock(TestingAuthenticationToken.class);

            doReturn(authentication)
                .when(securityUtilsSpy).getAuthenticationToken();
            when(authentication.getName())
                .thenReturn(username);

            //Act and Assert
            assertThatThrownBy(() -> securityUtilsSpy.getAuthenticatedUsername())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Authenticated username is not specified");

            verify(securityUtilsSpy).getAuthenticationToken();
        }

        @Test
        @DisplayName("Should return authenticated username when authenticated username is not null or blank")
        void shouldReturnUsername_whenAuthenticatedUsernamePresent(){
            //Arrange
            String username = "username";
            TestingAuthenticationToken authentication = new TestingAuthenticationToken(username, "password", User.UserRole.CUSTOMER.name());

            doReturn(authentication)
                .when(securityUtilsSpy).getAuthenticationToken();

            //Act
            String authenticatedUsername = securityUtilsSpy.getAuthenticatedUsername();

            //Assert
            assertThat(authenticatedUsername).isNotNull();
            assertThat(authenticatedUsername).isEqualTo(username);
        }

    }

    @Nested
    @DisplayName("getAuthenticatedUserRole tests")
    class GetAuthenticatedUserRoleTests {

        @ParameterizedTest
        @MethodSource("provideAuthoritiesCollections")
        @DisplayName("Should throw IllegalStateException when authenticated user has no roles")
        void shouldThrowIllegalStateException_whenAuthenticatedUserHasNoRole(Collection<GrantedAuthority> authorities){
            //Arrange
            TestingAuthenticationToken authentication = new TestingAuthenticationToken("username", "password", authorities);

            doReturn(authentication)
                .when(securityUtilsSpy).getAuthenticationToken();

            //Act and Assert
            assertThatThrownBy(() -> securityUtilsSpy.getAuthenticatedUserRole())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Authenticated user has no role");

            verify(securityUtilsSpy).getAuthenticationToken();
        }

        @Test
        @DisplayName("Should throw IllegalStateException when authenticated user has more than one role")
        void shouldThrowIllegalStateException_whenAuthenticatedUserHasMoreThanOneRole(){
            //Arrange
            Collection<GrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_a"),
                new SimpleGrantedAuthority("ROLE_b"),
                new SimpleGrantedAuthority("ROLE_c")
            );
            TestingAuthenticationToken authentication = new TestingAuthenticationToken("username", "password", authorities);

            doReturn(authentication)
                .when(securityUtilsSpy).getAuthenticationToken();

            //Act and Assert
            assertThatThrownBy(() -> securityUtilsSpy.getAuthenticatedUserRole())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Authenticated user has more than 1 role " + authorities.stream().map(GrantedAuthority::getAuthority).toList());

            verify(securityUtilsSpy).getAuthenticationToken();
        }

        @Test
        @DisplayName("Should return authenticated user role when authenticated user has only one role")
        void shouldReturnRole_whenAuthenticatedUserHasOnlyOneRole(){
            //Arrange
            String role = User.UserRole.CUSTOMER.name();
            TestingAuthenticationToken authentication = new TestingAuthenticationToken("username", "password", "ROLE_" + role);

            doReturn(authentication)
                .when(securityUtilsSpy).getAuthenticationToken();

            //Act
            String userRole = securityUtilsSpy.getAuthenticatedUserRole();

            //Assert
            assertThat(userRole).isNotNull();
            assertThat(userRole).isEqualTo(role);

            verify(securityUtilsSpy).getAuthenticationToken();
        }

        /*
                Test data provider methods
         */

        static Stream<Arguments> provideAuthoritiesCollections(){
            return Stream.of(
                //Empty authorities
                Arguments.of(List.of()),
                //Non-role authorities
                Arguments.of(Stream.of("SCP_a", "permission_b", "something_else").map(SimpleGrantedAuthority::new).toList())
            );
        }

    }

    @Nested
    @DisplayName("getAuthenticationToken tests")
    class GetAuthenticationTokenTests{

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
            assertThatThrownBy(() -> securityUtils.getAuthenticationToken())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("User is not authenticated");

            securityContextHolder.verify(SecurityContextHolder::getContext);
            verify(securityContext).getAuthentication();
        }

        @Test
        @DisplayName("Should return the authentication when user is authenticated")
        void shouldReturnAuthentication_whenUserAuthenticated(){
            //Arrange
            Authentication authentication = new TestingAuthenticationToken("username", "password", "ROLE_" + User.UserRole.CUSTOMER.name());

            securityContextHolder.when(SecurityContextHolder::getContext)
                .thenReturn(securityContext);
            when(securityContext.getAuthentication())
                .thenReturn(authentication);

            //Act
            Authentication result = securityUtils.getAuthenticationToken();

            //Assert
            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo(authentication.getName());

            securityContextHolder.verify(SecurityContextHolder::getContext);
            verify(securityContext).getAuthentication();
        }

        /*
                Test data provider methods
         */

        static Stream<Arguments> provideInvalidAuthentications(){
            return Stream.of(
                //Not authenticated authentication
                Arguments.of(new TestingAuthenticationToken("username", "password"))
            );
        }

    }

}
