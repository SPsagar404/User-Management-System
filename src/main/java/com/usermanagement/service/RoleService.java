package com.usermanagement.service;

import com.usermanagement.dto.request.RoleRequest;
import com.usermanagement.dto.response.RoleResponse;
import com.usermanagement.entity.Role;
import com.usermanagement.exception.DuplicateResourceException;
import com.usermanagement.mapper.UserMapper;
import com.usermanagement.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;

    @Transactional
    public RoleResponse createRole(RoleRequest request) {
        String roleName = request.getName().toUpperCase();

        // Ensure ROLE_ prefix
        if (!roleName.startsWith("ROLE_")) {
            roleName = "ROLE_" + roleName;
        }

        if (roleRepository.existsByName(roleName)) {
            throw new DuplicateResourceException("Role already exists: " + roleName);
        }

        Role role = Role.builder()
                .name(roleName)
                .build();

        Role savedRole = roleRepository.save(role);
        log.info("Created role: {}", savedRole.getName());

        return UserMapper.toRoleResponse(savedRole);
    }
}
