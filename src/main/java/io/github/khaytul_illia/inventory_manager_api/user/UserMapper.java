package io.github.khaytul_illia.inventory_manager_api.user;

import io.github.khaytul_illia.inventory_manager_api.user.response.UserResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserMapper {

    @Mapping(target = "id", ignore = true)
    User buildUser(String username, String password, User.UserRole role);

    UserResponse toUserResponse(User user);

}
