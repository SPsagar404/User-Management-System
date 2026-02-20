package com.usermanagement.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.usermanagement.TestConfig;
import com.usermanagement.dto.request.LoginRequest;
import com.usermanagement.entity.Role;
import com.usermanagement.entity.User;
import com.usermanagement.repository.RoleRepository;
import com.usermanagement.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestConfig.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AdminControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static String adminToken;
    private static String userToken;

    @BeforeEach
    void setUp() {
        if (!roleRepository.existsByName("ROLE_USER")) {
            roleRepository.save(Role.builder().name("ROLE_USER").build());
        }
        if (!roleRepository.existsByName("ROLE_ADMIN")) {
            roleRepository.save(Role.builder().name("ROLE_ADMIN").build());
        }
    }

    private void createAdminAndLogin() throws Exception {
        if (adminToken != null)
            return;

        if (userRepository.findByEmail("statsadmin@example.com").isEmpty()) {
            User adminUser = User.builder()
                    .username("statsadmin")
                    .email("statsadmin@example.com")
                    .password(passwordEncoder.encode("admin123"))
                    .build();
            adminUser.getRoles().add(roleRepository.findByName("ROLE_ADMIN").orElseThrow());
            adminUser.getRoles().add(roleRepository.findByName("ROLE_USER").orElseThrow());
            userRepository.save(adminUser);
        }

        LoginRequest loginRequest = LoginRequest.builder()
                .email("statsadmin@example.com")
                .password("admin123")
                .build();

        MvcResult result = mockMvc.perform(post("/api/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        adminToken = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("token").asText();
    }

    private void createRegularUserAndLogin() throws Exception {
        if (userToken != null)
            return;

        if (userRepository.findByEmail("statsuser@example.com").isEmpty()) {
            User regularUser = User.builder()
                    .username("statsuser")
                    .email("statsuser@example.com")
                    .password(passwordEncoder.encode("user123"))
                    .build();
            regularUser.getRoles().add(roleRepository.findByName("ROLE_USER").orElseThrow());
            userRepository.save(regularUser);
        }

        LoginRequest loginRequest = LoginRequest.builder()
                .email("statsuser@example.com")
                .password("user123")
                .build();

        MvcResult result = mockMvc.perform(post("/api/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        userToken = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("token").asText();
    }

    @Test
    @Order(1)
    @DisplayName("GET /api/admin/stats — should return 401 without token")
    void shouldReturn401WithoutToken() throws Exception {
        mockMvc.perform(get("/api/admin/stats"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(2)
    @DisplayName("GET /api/admin/stats — should return 403 for non-admin user")
    void shouldReturn403ForNonAdminUser() throws Exception {
        createRegularUserAndLogin();

        mockMvc.perform(get("/api/admin/stats")
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(3)
    @DisplayName("GET /api/admin/stats — should return stats for admin")
    void shouldReturnStatsForAdmin() throws Exception {
        createAdminAndLogin();

        mockMvc.perform(get("/api/admin/stats")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalUsers").isNumber());
    }
}
