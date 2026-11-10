package io.github.khaytul_illia.inventory_manager_api.support.utils;

import io.github.khaytul_illia.inventory_manager_api.support.data.TestDataConstants;
import io.github.khaytul_illia.inventory_manager_api.user.User;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collection;

public class TestAuthenticationUtils {

    public static void persistAuditedEntities(Runnable operation){
        setOperatorAuthentication();
        
        operation.run();

        resetAuthentication();
    }
    
    public static void setCustomerAuthentication(){
        SecurityContextHolder.getContext().setAuthentication(
            buildCustomerAuthentication()
        );
    }

    public static void setOperatorAuthentication(){
        SecurityContextHolder.getContext().setAuthentication(
            buildOperatorAuthentication()
        );
    }
    
    public static void resetAuthentication(){
        SecurityContextHolder.getContext().setAuthentication(null);
    }
    
    public static TestingAuthenticationToken buildAuthentication(Collection<GrantedAuthority> authorities){
        return new TestingAuthenticationToken(TestDataConstants.USER_USERNAME, TestDataConstants.USER_PASSWORD, authorities);
    }
    
    public static TestingAuthenticationToken buildCustomerAuthentication(){
        return new TestingAuthenticationToken(TestDataConstants.USER_USERNAME, TestDataConstants.USER_PASSWORD, "ROLE_" + User.UserRole.CUSTOMER.name());
    }

    public static TestingAuthenticationToken buildOperatorAuthentication(){
        return new TestingAuthenticationToken(TestDataConstants.USER_USERNAME, TestDataConstants.USER_PASSWORD, "ROLE_" + User.UserRole.OPERATOR.name());
    }

}
