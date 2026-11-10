package io.github.khaytul_illia.inventory_manager_api.support.data;

import io.github.khaytul_illia.inventory_manager_api.auth.session.UserSession;
import io.github.khaytul_illia.inventory_manager_api.user.User;

import java.time.Instant;

public class UserSessionTestDataFactory {

    public static UserSession newUserSession(User user){
        return defaults().user(user).build();
    }

    public static UserSession newUserSession(boolean valid, Instant createdAt, User user){
        return builder()
            .valid(valid)
            .createdAt(createdAt)
            .expiresAt(createdAt.plusSeconds(3600))
            .user(user)
            .build();
    }

    public static UserSession existingUserSession(){
        return defaults()
            .id(1L)
            .user(UserTestDataFactory.existingDefaultUser())
            .build();
    }

    public static UserSession existingUserSession(User user){
        return defaults().id().user(user).build();
    }

    public static UserSession existingUserSession(boolean valid, Instant createdAt, User user){
        return builder()
            .id()
            .valid(valid)
            .createdAt(createdAt)
            .expiresAt(createdAt.plusSeconds(3600))
            .user(user)
            .build();
    }

    public static UserSessionTestDataBuilder defaults(){
        return builder()
            .valid(true)
            .createdAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600));
    }

    public static UserSessionTestDataBuilder builder(){
        return new UserSessionTestDataBuilder();
    }
    
    public static class UserSessionTestDataBuilder{
        
        private Long id;
        private boolean valid;
        private Instant createdAt;
        private Instant expiresAt;
        private User user;

        public UserSessionTestDataBuilder id(Long id) {
            this.id = id;

            return this;
        }

        public UserSessionTestDataBuilder id() {
            this.id = TestDataConstants.uniqueValue.getAndIncrement();

            return this;
        }

        public UserSessionTestDataBuilder valid(boolean valid) {
            this.valid = valid;

            return this;
        }

        public UserSessionTestDataBuilder createdAt(Instant createdAt) {
            this.createdAt = createdAt;

            return this;
        }

        public UserSessionTestDataBuilder expiresAt(Instant expiresAt) {
            this.expiresAt = expiresAt;

            return this;
        }

        public UserSessionTestDataBuilder user(User user) {
            this.user = user;

            return this;
        }

        public UserSession build(){
            return new UserSession(id, valid, createdAt, expiresAt, user);
        }

    }
    
}
