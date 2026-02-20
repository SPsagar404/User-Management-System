package com.usermanagement.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.usermanagement.TestConfig;
import com.usermanagement.dto.request.AssignRoleRequest;
import com.usermanagement.dto.request.LoginRequest;
import com.usermanagement.dto.request.RegisterRequest;
import com.usermanagement.dto.request.RoleRequest;
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

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestConfig.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class UserControllerIntegrationTest {

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

    private static String userToken;
    private static String adminToken;
    private static Long userId;

    @BeforeEach
    void setUp() {
        // Ensure ROLE_USER exists for registration
        if (!roleRepository.existsByName("ROLE_USER")) {
            roleRepository.save(Role.builder().name("ROLE_USER").build());
        }
    }

    @Test
    @Order(1)
    @DisplayName("POST /api/users/register — should register user and return 201 with JWT")
    void shouldRegisterUser() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .username("testuser")
                .email("test@example.com")
                .password("password123")
                .build();

        MvcResult result = mockMvc.perform(post("/api/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.email").value("test@example.com"))
                .andReturn();

        // Extract token for subsequent tests
        String responseBody = result.getResponse().getContentAsString();
        userToken = objectMapper.readTree(responseBody).path("data").path("token").asText();
        userId = objectMapper.readTree(responseBody).path("data").path("userId").asLong();
    }

    @Test
    @Order(2)
    @DisplayName("POST /api/users/register — should fail with duplicate email (409)")
    void shouldFailRegisterDuplicateEmail() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .username("testuser2")
                .email("test@example.com")
                .password("password123")
                .build();

        mockMvc.perform(post("/api/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @Order(3)
    @DisplayName("POST /api/users/register — should fail with invalid input (400)")
    void shouldFailRegisterWithInvalidInput() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .username("")
                .email("invalid-email")
                .password("12")
                .build();

        mockMvc.perform(post("/api/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @Order(4)
    @DisplayName("POST /api/users/login — should login and return JWT")
    void shouldLoginUser() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("test@example.com")
                .password("password123")
                .build();

        MvcResult result = mockMvc.perform(post("/api/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        userToken = objectMapper.readTree(responseBody).path("data").path("token").asText();
    }

    @Test
    @Order(5)
    @DisplayName("POST /api/users/login — should fail with wrong password (401)")
    void shouldFailLoginWithWrongPassword() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("test@example.com")
                .password("wrongpassword")
                .build();

        mockMvc.perform(post("/api/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(6)
    @DisplayName("GET /api/users/me — should return 401 without token")
    void shouldReturn401WithoutToken() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(7)
    @DisplayName("GET /api/users/me — should return user profile with valid token")
    void shouldReturnUserProfile() throws Exception {
        mockMvc.perform(get("/api/users/me")
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("test@example.com"))
                .andExpect(jsonPath("$.data.username").value("testuser"))
                .andExpect(jsonPath("$.data.roles").isArray());
    }

    @Test
    @Order(8)
    @DisplayName("POST /api/users/{userId}/roles — should return 403 without ADMIN role")
    void shouldForbidRoleAssignmentWithoutAdmin() throws Exception {
        AssignRoleRequest request = AssignRoleRequest.builder()
                .roleName("ADMIN")
                .build();

        mockMvc.perform(post("/api/users/" + userId + "/roles")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(9)
    @DisplayName("Setup admin user and test admin-only endpoints")
    void shouldAllowAdminToAssignRoles() throws Exception {
        // Create ADMIN role
        if (!roleRepository.existsByName("ROLE_ADMIN")) {
            roleRepository.save(Role.builder().name("ROLE_ADMIN").build());
        }

        // Create an admin user directly in the DB
        User adminUser = User.builder()
                .username("admin")
                .email("admin@example.com")
                .password(passwordEncoder.encode("admin123"))
                .build();

        Role adminRole = roleRepository.findByName("ROLE_ADMIN").orElseThrow();
        Role userRole = roleRepository.findByName("ROLE_USER").orElseThrow();
        adminUser.getRoles().add(adminRole);
        adminUser.getRoles().add(userRole);
        userRepository.save(adminUser);

        // Login as admin
        LoginRequest loginRequest = LoginRequest.builder()
                .email("admin@example.com")
                .password("admin123")
                .build();

        MvcResult loginResult = mockMvc.perform(post("/api/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        adminToken = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .path("data").path("token").asText();

        // Admin assigns ADMIN role to the test user
        AssignRoleRequest assignRequest = AssignRoleRequest.builder()
                .roleName("ADMIN")
                .build();

        mockMvc.perform(post("/api/users/" + userId + "/roles")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(assignRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.roles", hasItem("ROLE_ADMIN")));
    }

    @Test
    @Order(10)
    @DisplayName("POST /api/roles — should return 403 for non-admin user")
    void shouldForbidRoleCreationForNonAdmin() throws Exception {
        // Register a non-admin user
        RegisterRequest request = RegisterRequest.builder()
                .username("regularuser")
                .email("regular@example.com")
                .password("password123")
                .build();

        MvcResult result = mockMvc.perform(post("/api/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String regularToken = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("token").asText();

        RoleRequest roleRequest = RoleRequest.builder().name("MODERATOR").build();

        mockMvc.perform(post("/api/roles")
                .header("Authorization", "Bearer " + regularToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(roleRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(11)
    @DisplayName("POST /api/roles — admin should create role successfully")
    void shouldAllowAdminToCreateRole() throws Exception {
        RoleRequest roleRequest = RoleRequest.builder().name("MODERATOR").build();

        mockMvc.perform(post("/api/roles")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(roleRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("ROLE_MODERATOR"));
    }
}
