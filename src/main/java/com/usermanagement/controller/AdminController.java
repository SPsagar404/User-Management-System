package com.usermanagement.controller;

import com.usermanagement.dto.response.ApiResponse;
import com.usermanagement.dto.response.StatsResponse;
import com.usermanagement.entity.AuditLog;
import com.usermanagement.repository.AuditLogRepository;
import com.usermanagement.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Admin Dashboard", description = "Admin-only statistics and management endpoints")
public class AdminController {

    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get system statistics (ADMIN only)", description = "Returns total user count and last login timestamp", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<StatsResponse>> getStats() {
        long totalUsers = userRepository.count();

        LocalDateTime lastLoginTimestamp = auditLogRepository
                .findTopByActionOrderByTimestampDesc("USER_LOGGED_IN")
                .map(AuditLog::getTimestamp)
                .orElse(null);

        StatsResponse stats = StatsResponse.builder()
                .totalUsers(totalUsers)
                .lastLoginTimestamp(lastLoginTimestamp)
                .build();

        return ResponseEntity.ok(ApiResponse.success("Admin stats retrieved", stats));
    }
}
