package com.usermanagement.mapper;

import com.usermanagement.dto.response.RoleResponse;
import com.usermanagement.dto.response.UserResponse;
import com.usermanagement.entity.Role;
import com.usermanagement.entity.User;

import java.util.stream.Collectors;

public final class UserMapper {

    private UserMapper() {
        // Utility class — no instantiation
    }

    public static UserResponse toUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .roles(user.getRoles().stream()
                        .map(Role::getName)
                        .collect(Collectors.toSet()))
                .createdAt(user.getCreatedAt())
                .build();
    }

    public static RoleResponse toRoleResponse(Role role) {
        return RoleResponse.builder()
                .id(role.getId())
                .name(role.getName())
                .build();
    }
}
