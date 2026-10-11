package io.github.khaytul_illia.inventory_manager_api.common.response.view;

import com.fasterxml.jackson.annotation.JsonView;
import io.github.khaytul_illia.inventory_manager_api.security.SecurityUtils;
import io.github.khaytul_illia.inventory_manager_api.user.User;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractJacksonHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.util.Map;

@RestControllerAdvice
public class RoleBasedViewResolver implements ResponseBodyAdvice<Object> {

    private final SecurityUtils securityUtils;

    public RoleBasedViewResolver(SecurityUtils securityUtils) {
        this.securityUtils = securityUtils;
    }

    @Override
    public boolean supports(
        MethodParameter returnType,
        Class<? extends HttpMessageConverter<?>> converterType
    ) {
        return returnType.hasMethodAnnotation(RoleBasedJsonView.class) && AbstractJacksonHttpMessageConverter.class.isAssignableFrom(converterType);
    }

    @Override
    public Object beforeBodyWrite(
        Object body,
        MethodParameter returnType,
        MediaType selectedContentType,
        Class<? extends HttpMessageConverter<?>> selectedConverterType,
        ServerHttpRequest request,
        ServerHttpResponse response
    ) {
        return body;
    }

    @Override
    public Map<String, Object> determineWriteHints(
        Object body,
        MethodParameter returnType,
        MediaType selectedContentType,
        Class<? extends HttpMessageConverter<?>> selectedConverterType
    ) {
        String role;
        try {
            role = securityUtils.getAuthenticatedUserAccessToken().getClaimAsString("roles");
        } catch (Exception ignored) {
            role = null;
        }

        if (User.UserRole.OPERATOR.name().equalsIgnoreCase(role)) {
            return Map.of();
        }

        return Map.of(JsonView.class.getName(), ResponseViews.RestrictedView.class);
    }

}
