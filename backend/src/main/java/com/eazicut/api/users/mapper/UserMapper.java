package com.eazicut.api.users.mapper;

import org.mapstruct.Mapper;

import com.eazicut.api.users.dto.UserResponse;
import com.eazicut.api.users.entity.User;

/**
 * MapStruct mapper for {@link User} → {@link UserResponse}.
 *
 * <p>Deliberately one-way. The reverse ({@code toEntity}) is not
 * generated — every persist path constructs {@code User} explicitly so
 * password hashing, role assignment, and email normalisation happen at
 * the intended service boundary rather than via a general-purpose
 * mapping.
 */
@Mapper(componentModel = "spring")
public interface UserMapper {

    UserResponse toResponse(User user);
}
