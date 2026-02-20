package com.usermanagement.service;

import com.usermanagement.dto.request.AssignRoleRequest;
import com.usermanagement.dto.request.LoginRequest;
import com.usermanagement.dto.request.RegisterRequest;
import com.usermanagement.dto.response.AuthResponse;
import com.usermanagement.dto.response.UserResponse;
import com.usermanagement.entity.Role;
import com.usermanagement.entity.User;
import com.usermanagement.event.EventPublisher;
import com.usermanagement.event.UserEvent;
import com.usermanagement.exception.BadRequestException;
import com.usermanagement.exception.DuplicateResourceException;
import com.usermanagement.exception.ResourceNotFoundException;
import com.usermanagement.mapper.UserMapper;
import com.usermanagement.repository.RoleRepository;
import com.usermanagement.repository.UserRepository;
import com.usermanagement.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

        private final UserRepository userRepository;
        private final RoleRepository roleRepository;
        private final PasswordEncoder passwordEncoder;
        private final AuthenticationManager authenticationManager;
        private final JwtTokenProvider jwtTokenProvider;
        private final EventPublisher eventPublisher;
        private final AuditLogService auditLogService;

        @Transactional
        public AuthResponse register(RegisterRequest request) {
                if (userRepository.existsByEmail(request.getEmail())) {
                        throw new DuplicateResourceException("Email is already registered: " + request.getEmail());
                }

                User user = User.builder()
                                .username(request.getUsername())
                                .email(request.getEmail())
                                .password(passwordEncoder.encode(request.getPassword()))
                                .build();

                // Assign default USER role if it exists
                roleRepository.findByName("ROLE_USER").ifPresent(role -> user.getRoles().add(role));

                User savedUser = userRepository.save(user);
                log.info("User registered successfully: {}", savedUser.getEmail());

                // Publish registration event
                eventPublisher.publishRegistrationEvent(UserEvent.builder()
                                .eventType(UserEvent.EventType.USER_REGISTERED.name())
                                .userId(savedUser.getId())
                                .email(savedUser.getEmail())
                                .timestamp(LocalDateTime.now())
                                .build());

                // Audit log
                auditLogService.log("USER_REGISTERED", savedUser.getEmail(), savedUser.getEmail(),
                                "User registered successfully");

                // Auto-login after registration
                Authentication authentication = authenticationManager.authenticate(
                                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

                String token = jwtTokenProvider.generateToken(authentication);

                return AuthResponse.builder()
                                .token(token)
                                .tokenType("Bearer")
                                .userId(savedUser.getId())
                                .email(savedUser.getEmail())
                                .build();
        }

        @Transactional
        public AuthResponse login(LoginRequest request) {
                Authentication authentication = authenticationManager.authenticate(
                                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

                String token = jwtTokenProvider.generateToken(authentication);

                User user = userRepository.findByEmail(request.getEmail())
                                .orElseThrow(() -> new ResourceNotFoundException("User", "email", request.getEmail()));

                // Update last login timestamp
                user.setLastLoginAt(LocalDateTime.now());
                userRepository.save(user);

                // Publish login event
                eventPublisher.publishLoginEvent(UserEvent.builder()
                                .eventType(UserEvent.EventType.USER_LOGGED_IN.name())
                                .userId(user.getId())
                                .email(user.getEmail())
                                .timestamp(LocalDateTime.now())
                                .build());

                // Audit log
                auditLogService.log("USER_LOGGED_IN", user.getEmail(), user.getEmail(),
                                "User logged in successfully");

                log.info("User logged in: {}", user.getEmail());

                return AuthResponse.builder()
                                .token(token)
                                .tokenType("Bearer")
                                .userId(user.getId())
                                .email(user.getEmail())
                                .build();
        }

        @Transactional(readOnly = true)
        @Cacheable(value = "users", key = "#email")
        public UserResponse getCurrentUser(String email) {
                User user = userRepository.findByEmail(email)
                                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

                log.debug("Fetched current user: {}", email);
                return UserMapper.toUserResponse(user);
        }

        @Transactional
        @CacheEvict(value = "users", allEntries = true)
        public UserResponse assignRole(Long userId, AssignRoleRequest request) {
                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

                String roleName = request.getRoleName().toUpperCase();
                if (!roleName.startsWith("ROLE_")) {
                        roleName = "ROLE_" + roleName;
                }

                final String finalRoleName = roleName;
                Role role = roleRepository.findByName(finalRoleName)
                                .orElseThrow(() -> new ResourceNotFoundException("Role", "name", finalRoleName));

                if (user.getRoles().contains(role)) {
                        throw new BadRequestException("User already has role: " + role.getName());
                }

                user.getRoles().add(role);
                User updatedUser = userRepository.save(user);

                // Audit log
                auditLogService.log("ROLE_ASSIGNED", "ADMIN", user.getEmail(),
                                "Assigned role " + role.getName() + " to user " + user.getEmail());

                log.info("Role {} assigned to user {}", role.getName(), user.getEmail());

                return UserMapper.toUserResponse(updatedUser);
        }
}
