package io.github.khaytul_illia.inventory_manager_api.common.response.view;

import com.fasterxml.jackson.annotation.JsonView;
import io.github.khaytul_illia.inventory_manager_api.security.SecurityUtils;
import io.github.khaytul_illia.inventory_manager_api.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractJacksonHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RoleBasedViewResolver tests")
public class RoleBasedViewResolverTests {

    @Mock
    private SecurityUtils securityUtils;
    @InjectMocks
    private RoleBasedViewResolver viewResolver;

    @Nested
    @DisplayName("supports tests")
    class SupportsTests {

        @ParameterizedTest
        @MethodSource("provideReturnAndConvertedTypes")
        @DisplayName("Should return false when endpoint method is not annotated with RoleBasedJsonView or converter type is not AbstractJacksonHttpMessageConverter")
        void shouldReturnFalse_whenReturnOrConverterTypeUnsupported(boolean hasMethodAnnotation, Class<? extends HttpMessageConverter<?>> converterTypeClass) {
            //Arrange
            MethodParameter returnType = mock(MethodParameter.class);

            when(returnType.hasMethodAnnotation(RoleBasedJsonView.class))
                .thenReturn(hasMethodAnnotation);

            //Act
            boolean isSupported = viewResolver.supports(returnType, converterTypeClass);

            //Assert
            assertThat(isSupported).isFalse();
        }

        @Test
        @DisplayName("Should return true when endpoint method is annotated with RoleBasedJsonView and converter type is AbstractJacksonHttpMessageConverter")
        void shouldReturnTrue_whenReturnAndConverterTypeSupported() {
            //Arrange
            @SuppressWarnings("unchecked")
            Class<? extends HttpMessageConverter<?>> converterType = (Class<? extends HttpMessageConverter<?>>) (Class<?>) AbstractJacksonHttpMessageConverter.class;
            MethodParameter returnType = mock(MethodParameter.class);

            when(returnType.hasMethodAnnotation(RoleBasedJsonView.class))
                .thenReturn(true);

            //Act
            boolean isSupported = viewResolver.supports(returnType, converterType);

            //Assert
            assertThat(isSupported).isTrue();
        }

            /*
                Test data provider methods
         */

        static Stream<Arguments> provideReturnAndConvertedTypes() {
            return Stream.of(
                //Unsupported message converter type
                Arguments.of(
                    true, HttpMessageConverter.class
                ),
                //Has no JsonView method annotation
                Arguments.of(
                    false, AbstractJacksonHttpMessageConverter.class
                ),
                //Unsupported response and has no method annotation
                Arguments.of(
                    false, HttpMessageConverter.class
                )
            );
        }

    }

    @Nested
    @DisplayName("determineWriteHints tests")
    class DetermineWriteHintsTests {

        private final Object body = new Object();
        private MethodParameter returnType;
        private MediaType selectedContentType;
        private Class<? extends HttpMessageConverter<?>> selectedConverterType;
        private Jwt jwtMock;

        @BeforeEach
        void beforeEach() {
            returnType = mock(MethodParameter.class);
            selectedContentType = mock(MediaType.class);
            //noinspection unchecked
            selectedConverterType = (Class<? extends HttpMessageConverter<?>>) (Class<?>) AbstractJacksonHttpMessageConverter.class;
            jwtMock = mock(Jwt.class);
        }

        @ParameterizedTest
        @NullSource
        @ValueSource(strings = {"CUSTOMER"})
        @DisplayName("Should return RestrictedView hint when authenticated user is not an OPERATOR")
        void shouldReturnNoViewHint_whenRoleNotOperator(String role) {
            //Arrange
            when(securityUtils.getAuthenticatedUserAccessToken())
                .thenReturn(jwtMock);
            when(jwtMock.getClaimAsString("roles"))
                .thenReturn(role);

            //Act
            Map<String, Object> hints = viewResolver.determineWriteHints(body, returnType, selectedContentType, selectedConverterType);

            //Assert
            assertThat(hints).isNotNull();
            assertThat(hints).containsKey(JsonView.class.getName());
            assertThat(hints.get(JsonView.class.getName())).isEqualTo(ResponseViews.RestrictedView.class);
        }

        @Test
        @DisplayName("Should return RestrictedView hint when SecurityUtils throws")
        void shouldReturnNoViewHint_whenSecurityUtilsThrows() {
            //Arrange
            when(securityUtils.getAuthenticatedUserAccessToken())
                .thenThrow(new RuntimeException());

            //Act
            Map<String, Object> hints = viewResolver.determineWriteHints(body, returnType, selectedContentType, selectedConverterType);

            //Assert
            assertThat(hints).isNotNull();
            assertThat(hints).containsKey(JsonView.class.getName());
            assertThat(hints.get(JsonView.class.getName())).isEqualTo(ResponseViews.RestrictedView.class);
        }

        @Test
        @DisplayName("Should return no view hint when authenticated user is OPERATOR")
        void shouldReturnNoViewHint_whenRoleOperator() {
            //Arrange
            when(securityUtils.getAuthenticatedUserAccessToken())
                .thenReturn(jwtMock);
            when(jwtMock.getClaimAsString("roles"))
                .thenReturn(User.UserRole.OPERATOR.name());

            //Act
            Map<String, Object> hints = viewResolver.determineWriteHints(body, returnType, selectedContentType, selectedConverterType);

            //Assert
            assertThat(hints).isNotNull();
            assertThat(hints).isEmpty();
        }

    }

}
