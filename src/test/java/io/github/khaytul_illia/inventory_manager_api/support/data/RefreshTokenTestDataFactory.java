package io.github.khaytul_illia.inventory_manager_api.support.data;

import io.github.khaytul_illia.inventory_manager_api.auth.request.RefreshTokenRequest;
import io.github.khaytul_illia.inventory_manager_api.auth.response.AccessTokenResponse;
import io.github.khaytul_illia.inventory_manager_api.auth.session.UserSession;
import io.github.khaytul_illia.inventory_manager_api.auth.token.RefreshToken;
import io.github.khaytul_illia.inventory_manager_api.user.User;

import java.time.Instant;

public class RefreshTokenTestDataFactory {

    public static final RefreshTokenRequest DEFAULT_REFRESH_TOKEN_REQUEST = new RefreshTokenRequest(TestDataConstants.REFRESH_TOKEN_VALUE);
    public static final AccessTokenResponse DEFAULT_ACCESS_RESPONSE = new AccessTokenResponse(
        "issuer",
        Instant.now(),
        Instant.now().plusSeconds(900),
        TestDataConstants.USER_USERNAME,
        User.UserRole.CUSTOMER.name(),
        TestDataConstants.ACCESS_TOKEN_VALUE,
        TestDataConstants.REFRESH_TOKEN_VALUE
    );

    public static RefreshToken newUniqueRefreshToken(UserSession session){
        return defaults()
            .tokenValue()
            .session(session)
            .build();
    }

    public static RefreshToken newDefaultRefreshToken(UserSession session){
        return defaults().session(session).build();
    }

    public static RefreshToken existingUniqueRefreshToken(UserSession session){
        return defaults()
            .id()
            .tokenValue()
            .session(session)
            .build();
    }

    public static RefreshToken existingUniqueRefreshToken(boolean used, UserSession session){
        return defaults()
            .id()
            .tokenValue()
            .used(used)
            .session(session)
            .build();
    }

    public static RefreshToken existingDefaultRefreshToken(){
        return defaults()
            .id(1L)
            .session(UserSessionTestDataFactory.existingUserSession())
            .build();
    }

    public static RefreshTokenTestDataBuilder defaults(){
        return builder()
            .tokenValue(TestDataConstants.REFRESH_TOKEN_VALUE)
            .issuedAt(Instant.now())
            .used(false);
    }

    public static RefreshTokenTestDataBuilder builder(){
        return new RefreshTokenTestDataBuilder();
    }
    
    public static class RefreshTokenTestDataBuilder{
        
        private Long id;
        private String tokenValue;
        private Instant issuedAt;
        private boolean used;
        private UserSession session;

        public RefreshTokenTestDataBuilder id(Long id) {
            this.id = id;

            return this;
        }

        public RefreshTokenTestDataBuilder id() {
            this.id = TestDataConstants.uniqueValue.getAndIncrement();

            return this;
        }

        public RefreshTokenTestDataBuilder tokenValue(String tokenValue) {
            this.tokenValue = tokenValue;

            return this;
        }

        public RefreshTokenTestDataBuilder tokenValue() {
            this.tokenValue = TestDataConstants.REFRESH_TOKEN_VALUE + "_" + TestDataConstants.uniqueValue.getAndIncrement();

            return this;
        }

        public RefreshTokenTestDataBuilder issuedAt(Instant issuedAt) {
            this.issuedAt = issuedAt;

            return this;
        }

        public RefreshTokenTestDataBuilder used(boolean used) {
            this.used = used;

            return this;
        }

        public RefreshTokenTestDataBuilder session(UserSession session) {
            this.session = session;

            return this;
        }

        public RefreshToken build(){
            return new RefreshToken(id, tokenValue, issuedAt, used, session, 1);
        }

    }
    
}
