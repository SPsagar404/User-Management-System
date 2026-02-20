package com.usermanagement.service;

import com.usermanagement.entity.AuditLog;
import com.usermanagement.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Transactional
    public void log(String action, String performedBy, String targetUser, String details) {
        AuditLog auditLog = AuditLog.builder()
                .action(action)
                .performedBy(performedBy)
                .targetUser(targetUser)
                .details(details)
                .build();

        auditLogRepository.save(auditLog);
        log.info("Audit log: action={}, performedBy={}, target={}, details={}",
                action, performedBy, targetUser, details);
    }
}
