package com.hospital.his.audit.service;

import com.hospital.his.audit.entity.AuditLog;
import com.hospital.his.audit.repository.AuditLogRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository){
        this.auditLogRepository = auditLogRepository;
    }

    public void log(String username, String action, String module, String description){
        AuditLog auditLog = AuditLog.builder()
                .username(username)
                .action(action)
                .module(module)
                .description(description)
                .eventDate(LocalDateTime.now())
                .build();
    auditLogRepository.save(auditLog);
    }

}
